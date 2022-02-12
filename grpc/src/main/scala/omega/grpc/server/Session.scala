package omega.grpc.server

import akka.actor.{Actor, PoisonPill, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import io.grpc.Status
import omega.grpc.server.Session._
import omega.grpc.server.Sessions.{Err, Ok}
import omega.scaladsl.api

import java.nio.file.Path

object Session {
  def props(data: Option[Path]): Props = {
    val omega = data match {
      case None =>
        val session = OmegaLib.newSession(None)
        session.push(List.fill(10000)(" ").mkString)
        session
      case path => OmegaLib.newSession(path)
    }
    Props(new Session(omega))
  }

  trait Op
  case class Save(to: Path) extends Op
  case class View(offset: Long, capacity: Long) extends Op
  case class DestroyView(id: String) extends Op

  case class Push(data: String) extends Op
  case class Delete(offset: Long, length: Long) extends Op
  case class Insert(data: String, offset: Long) extends Op
  case class Overwrite(data: String, offset: Long) extends Op
}

class Session(session: api.Session) extends Actor {
  val sessionId: String = self.path.name

  def receive: Receive = {
    case View(off, cap) =>
      import context.system
      val vid = Viewport.Id.uuid()
      val fqid = s"$sessionId-$vid"

      val (ws, stream) = Source.queue[Viewport.Updated](10, OverflowStrategy.fail).preMaterialize()
      val v = session.viewCb(off, cap, v => ws.queue.offer(Viewport.Updated(fqid, v.data())))
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
  }
}
