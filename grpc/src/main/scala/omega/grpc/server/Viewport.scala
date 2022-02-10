package omega.grpc.server

import akka.actor.{Actor, ActorLogging, Props}
import com.google.protobuf.ByteString
import omega.grpc.server.Sessions.{Data, Ok}
import omega.grpc.server.Viewport.Get
import omega.scaladsl.api
import omega_edit.ObjectId

import java.util.UUID

object Viewport {
  def props(view: api.Viewport) = Props(new Viewport(view))

  case class Id(session: String, view: String)
  object Id {
    def unapply(oid: ObjectId): Option[(String, String)] =
      oid.id.split("-") match {
        case Array(s, v) => Some((s, v))
        case _           => None
      }

    def uuid(): String = UUID.randomUUID().toString.take(8)
  }

  trait Op
  case object Get extends Op
}

class Viewport(view: api.Viewport) extends Actor with ActorLogging {
  val viewportId: String = self.path.name

  def receive: Receive = {
    case Get =>
      sender() ! new Ok(viewportId) with Data {
        def data: ByteString = ByteString.copyFromUtf8(view.data())
      }
  }
}
