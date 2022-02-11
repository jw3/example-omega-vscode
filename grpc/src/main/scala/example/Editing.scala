package example

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import com.google.protobuf.ByteString
import omega.grpc.server.EditorService
import omega_edit._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Editing extends App {
  {
    // direct calls
    implicit val system: ActorSystem = ActorSystem("using-direct-calls")
    Await.ready(Demonstration(new EditorService()), Duration.Inf)
  }

  {
    // or client calls
    implicit val system: ActorSystem = ActorSystem("using-client-calls")

    val (iface, port) = ("localhost", 9000)
    val _ = EditorService.bind(iface, port)
    val client = EditorClient(GrpcClientSettings.connectToServiceAt(iface, port).withTls(false))
    Await.ready(Demonstration(client), Duration.Inf)
  }
}

object Demonstration {
  // both the client and service implement Editor trait
  // this is a great win for unit testing of the service
  def apply(svc: Editor)(implicit system: ActorSystem): Future[Unit] = {
    import system.dispatcher

    for {
      s <- svc.createSession(CreateSessionRequest(Some("build.sbt")))
      _ = println(s.getSessionId.id)
      v <- svc.createViewport(CreateViewportRequest(s.sessionId, 1000))
      _ = println(v.getViewportId.id)
      d <- svc.getViewportData(ViewportDataRequest(v.viewportId))
      _ = println(s"[source data] ${d.data.toStringUtf8.take(20)}...")
      _ <- svc.subscribeOnChangeViewport(v.getViewportId).take(1).runForeach(c => println(".:Viewport change event:."))
      _ <- svc.submitChange(
        ChangeRequest(s.sessionId, ChangeKind.CHANGE_OVERWRITE, 0, 0, Some(ByteString.copyFromUtf8("********")))
      )
      d <- svc.getViewportData(ViewportDataRequest(v.viewportId))
      _ = println(s"[edited data] ${d.data.toStringUtf8.take(20)}...")
      _ <- svc.getViewportData(ViewportDataRequest(None)).recover {
        case e =>
          // expecting viewport id required message
          println(s"err: $e")
      }
      _ = println()
      _ <- system.terminate()
    } yield ()
  }
}
