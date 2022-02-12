package omega.grpc.server

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.Source
import com.google.protobuf.ByteString
import omega.grpc.server.Sessions.{Data, Ok}
import omega.grpc.server.Viewport.{EventStream, Events, Get, Watch}
import omega.scaladsl.api
import omega.scaladsl.api.Change
import omega_edit.ObjectId

import java.util.UUID

object Viewport {
  type EventStream = Source[Viewport.Updated, NotUsed]
  trait Events {
    def stream: EventStream
  }

  def props(view: api.Viewport, stream: EventStream) = Props(new Viewport(view, stream))

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
  case object Watch extends Op
  case class Updated(id: String, data: String, change: Option[Change])
}

class Viewport(view: api.Viewport, events: EventStream) extends Actor with ActorLogging {
  val viewportId: String = self.path.name

  def receive: Receive = {
    case Get =>
      sender() ! new Ok(viewportId) with Data {
        def data: ByteString = ByteString.copyFromUtf8(view.data())
      }

    case Watch =>
      sender() ! new Ok(viewportId) with Events {
        def stream: EventStream = events
      }
  }
}
