package omega.server

import akka.actor.{Actor, Props}
import omega.scaladsl.api
import omega.server.protocol._

import java.nio.file.Path

object Session {
  def props(data: Option[Path]): Props =
    Props(new Session(OmegaLib.newSession(data)))
}

class Session(session: api.Session) extends Actor {
  def receive: Receive = {
    case ConnectView(ws, offset, length) =>
      session.viewCb(offset, length, v => ws ! ViewUpdated(v.data()))

    case Push(data)              => session.push(data)
    case Overwrite(data, offset) => session.overwrite(data, offset)
    case Delete(offset, length)  => session.delete(offset, length)
  }
}
