package omega.grpc

import akka.actor.ActorSystem
import omega.grpc.server.EditorService
import omega_edit.{CreateSessionRequest, CreateViewportRequest, ViewportDataRequest}

import scala.concurrent.ExecutionContext

object BasicEditorSvcTest extends App {
  implicit val sys: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = sys.dispatcher

  val svc = new EditorService()

  for {
    s <- svc.createSession(CreateSessionRequest(Some("build.sbt")))
    _ = println(s.getSessionId.id)
    v <- svc.createViewport(CreateViewportRequest(s.sessionId, 1000))
    _ = println(v.getViewportId.id)
    d <- svc.getViewportData(ViewportDataRequest(v.viewportId))
    _ = println(s"view data: ${d.data.toStringUtf8.take(20)}...")
  } {}
}
