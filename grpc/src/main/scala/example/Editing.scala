package example

import akka.actor.ActorSystem
import com.google.protobuf.ByteString
import omega.grpc.server.EditorService
import omega_edit._

import scala.concurrent.ExecutionContext

object Editing extends App {
  implicit val sys: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = sys.dispatcher

  val svc = new EditorService()

  for {
    s <- svc.createSession(CreateSessionRequest(Some("build.sbt")))
    _ = println(s.getSessionId.id)
    v <- svc.createViewport(CreateViewportRequest(s.sessionId, 1000))
    _ = println(v.getViewportId.id)
    d <- svc.getViewportData(ViewportDataRequest(v.viewportId))
    _ = println(s"original data: ${d.data.toStringUtf8.take(20)}...")
    _ <- svc.subscribeOnChangeViewport(v.getViewportId).take(1).runForeach(c => println(".:Viewport changed:."))
    _ <- svc.submitChange(
      ChangeRequest(s.sessionId, ChangeKind.CHANGE_OVERWRITE, 0, 0, Some(ByteString.copyFromUtf8("Hello!")))
    )
    d <- svc.getViewportData(ViewportDataRequest(v.viewportId))
    _ = println(s"updated data: ${d.data.toStringUtf8.take(20)}...")
    _ <- svc.getViewportData(ViewportDataRequest(None)).recover {
      case e =>
        // expecting viewport id required message
        println(s"err: $e")
    }
  } {}
}
