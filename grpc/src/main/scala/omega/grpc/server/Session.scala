package omega.grpc.server

import akka.actor.{Actor, Props}
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
}

class Session(session: api.Session) extends Actor {
  def receive: Receive = {
    case _ =>
  }
}
