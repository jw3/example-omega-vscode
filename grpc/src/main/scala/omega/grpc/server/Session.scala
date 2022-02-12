package omega.grpc.server

import akka.NotUsed
import akka.actor.{Actor, PoisonPill, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import io.grpc.Status
import omega.grpc.server.Session._
import omega.grpc.server.Sessions.{Err, Ok}
import omega.scaladsl.api
import omega.scaladsl.api.Change

import java.nio.file.Path

object Session {
  type EventStream = Source[Session.Updated, NotUsed]
  trait Events {
    def stream: EventStream
  }

  def props(session: api.Session, events: EventStream): Props =
    Props(new Session(session, events))

  trait Op
  case class Save(to: Path) extends Op
  case class View(offset: Long, capacity: Long) extends Op
  case class DestroyView(id: String) extends Op
  case object Watch extends Op

  case class Push(data: String) extends Op
  case class Delete(offset: Long, length: Long) extends Op
  case class Insert(data: String, offset: Long) extends Op
  case class Overwrite(data: String, offset: Long) extends Op

  case class Updated(id: String)

  case class LookupChange(id: Long) extends Op
  trait ChangeDetails {
    def change: Change
  }
}

class Session(session: api.Session, events: EventStream) extends Actor {
  val sessionId: String = self.path.name

  def receive: Receive = {
    case View(off, cap) =>
      import context.system
      val vid = Viewport.Id.uuid()
      val fqid = s"$sessionId-$vid"

      val (input, stream) = Source.queue[Viewport.Updated](10, OverflowStrategy.fail).preMaterialize()
      val v = session.viewCb(off, cap, (v, c) => input.queue.offer(Viewport.Updated(fqid, v.data(), c)))
      context.actorOf(Viewport.props(v, stream), vid)

      sender() ! Ok(fqid)

    case DestroyView(vid) =>
      context.child(vid) match {
        case None => sender() ! Err(Status.NOT_FOUND)
        case Some(s) =>
          s ! PoisonPill
          sender() ! Ok(vid)
      }

    case Push(data) =>
      session.push(data)
      sender() ! Ok(sessionId)

    case Insert(data, offset) =>
      session.insert(data, offset)
      sender() ! Ok(sessionId)

    case Overwrite(data, offset) =>
      session.overwrite(data, offset)
      sender() ! Ok(sessionId)

    case Delete(offset, length) =>
      session.delete(offset, length)
      sender() ! Ok(sessionId)

    case LookupChange(id) =>
      session.findChange(id) match {
        case Some(c) =>
          new Ok(s"$id") with ChangeDetails {
            def change: Change = c
          }
        case None => sender() ! Err(Status.NOT_FOUND)
      }

    case Watch =>
      sender() ! new Ok(sessionId) with Events {
        def stream: EventStream = events
      }
  }
}
