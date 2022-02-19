package omega.grpc.server

import akka.NotUsed
import akka.actor.{Actor, PoisonPill, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import io.grpc.Status
import omega.grpc.server.Session._
import omega.grpc.server.Sessions.{Err, Ok}
import com.ctc.omega_edit.api
import com.ctc.omega_edit.api.{Change, SessionCallback, ViewportCallback}

import java.nio.file.Path

object Session {
  type EventStream = Source[Session.Updated, NotUsed]
  trait Events {
    def stream: EventStream
  }

  def props(session: api.Session, events: EventStream, cb: SessionCallback): Props =
    Props(new Session(session, events, cb))

  trait Op
  case class Save(to: Path) extends Op
  case class View(offset: Long, capacity: Long, id: Option[String]) extends Op
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

class Session(session: api.Session, events: EventStream, cb: SessionCallback) extends Actor {
  val sessionId: String = self.path.name

  def receive: Receive = {
    case View(off, cap, id) =>
      import context.system
      val vid = id.getOrElse(Viewport.Id.uuid())
      val fqid = s"$sessionId-$vid"

      context.child(fqid) match {
        case Some(_) => sender() ! Err(Status.ALREADY_EXISTS)
        case None =>
          val (input, stream) = Source.queue[Viewport.Updated](10, OverflowStrategy.fail).preMaterialize()
          val cb = ViewportCallback((v, e, c) => input.queue.offer(Viewport.Updated(fqid, v.data(), c)))
          context.actorOf(Viewport.props(session.viewCb(off, cap, cb), stream, cb), vid)
          sender() ! Ok(fqid)
      }

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
