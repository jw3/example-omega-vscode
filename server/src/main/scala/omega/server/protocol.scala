package omega.server

import akka.actor.ActorRef
import spray.json.derived.Discriminator
import spray.json.derived.semiauto.deriveFormat
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object protocol {
  case class ViewUpdated(data: String)
  case class ConnectView(ws: ActorRef, offset: Long, length: Long)

  case class Push(data: String) extends SessionOp
  case class Overwrite(data: String, offset: Long) extends SessionOp
  case class Delete(offset: Long, length: Long) extends SessionOp

  @Discriminator("op")
  sealed trait SessionOp
  object SessionOp extends DefaultJsonProtocol {
    implicit val format: RootJsonFormat[SessionOp] = new RootJsonFormat[SessionOp] {
      val f = deriveFormat[SessionOp]
      def read(json: JsValue): SessionOp = f.read(json)
      def write(obj: SessionOp): JsValue = f.write(obj)
    }
  }
}
