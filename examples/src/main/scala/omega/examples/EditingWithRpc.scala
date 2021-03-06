package omega.examples

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import com.google.protobuf.ByteString
import omega.grpc.server.EditorService
import omega_edit.{
  ChangeKind,
  ChangeRequest,
  CreateSessionRequest,
  CreateViewportRequest,
  Editor,
  EditorClient,
  ObjectId,
  ViewportDataRequest
}

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
  def apply(editor: Editor)(implicit system: ActorSystem): Future[Unit] = {
    import system.dispatcher

    for {
      s <- editor.createSession(CreateSessionRequest(Some("build.sbt")))
      _ = println(s.sessionId)
      _ = editor.subscribeToSessionEvents(ObjectId(s.sessionId)).runForeach(_ => println(".:Session change event:."))
      v <- editor.createViewport(CreateViewportRequest(s.sessionId, 1000))
      _ = println(v.viewportId)
      d <- editor.getViewportData(ViewportDataRequest(v.viewportId))
      _ = println(s"[source data] ${d.data.toStringUtf8.take(20)}...")
      _ = editor.subscribeToViewportEvents(ObjectId(v.viewportId)).runForeach(_ => println(".:Viewport change event:."))
      _ <- editor.submitChange(
        ChangeRequest(s.sessionId, ChangeKind.CHANGE_OVERWRITE, 0, 0, Some(ByteString.copyFromUtf8("********")))
      )
      d <- editor.getViewportData(ViewportDataRequest(v.viewportId))
      _ = println(s"[edited data] ${d.data.toStringUtf8.take(20)}...")
      _ <- editor.getViewportData(ViewportDataRequest("")).recover {
        case e =>
          // expecting viewport id required message
          println(s"err: $e")
      }
      _ = println()
      _ = system.terminate()
    } yield ()
  }
}
