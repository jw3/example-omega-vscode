package omega.grpc.server

import akka.actor.{Actor, PoisonPill, Props}
import io.grpc.Status
import omega.grpc.server.Session.{DestroyView, View}
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
}

class Session(session: api.Session) extends Actor {
  val sessionId: String = self.path.name

  def receive: Receive = {
    case View(off, cap) =>
      val v = session.view(off, cap)
      val vid = Viewport.Id.uuid()
      context.actorOf(Viewport.props(v), vid)
      println(s"created viewport $vid")
      sender() ! Ok(s"$sessionId-$vid")

    case DestroyView(vid) =>
      context.child(vid) match {
        case None => sender() ! Err(Status.NOT_FOUND)
        case Some(s) =>
          s ! PoisonPill
          sender() ! Ok(vid)
      }
  }
}
