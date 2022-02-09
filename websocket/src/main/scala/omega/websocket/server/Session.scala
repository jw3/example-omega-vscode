package omega.websocket.server

import akka.actor.{Actor, Props}
import omega.scaladsl.api
import protocol._

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
    case ConnectView(ws, view) =>
      session.viewCb(view.offset, view.length, v => {
        println(s"fired ${view.id}!")
        ws ! ViewUpdated(view.id, v.data())
      })
      println(s"registered view: $view")

    case Push(data)              => session.push(data)
    case Overwrite(data, offset) => session.overwrite(data, offset)
    case Delete(offset, length)  => session.delete(offset, length)
  }
}
