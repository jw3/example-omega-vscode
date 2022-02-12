package omega.grpc.server

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.google.protobuf.ByteString
import io.grpc.Status
import omega.scaladsl.api
import omega.scaladsl.api.SessionCallback

import java.nio.file.Path
import java.util.{Base64, UUID}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Sessions {
  def props() = Props(new Sessions)

  case class Find(id: String)
  case class Create(path: Option[Path])
  case class Destroy(id: String)

  ///

  case class SessionOp(id: String, op: Session.Op)
  case class ViewportOp(sid: String, vid: String, op: Viewport.Op)

  sealed trait Result
  case class Ok(id: String) extends Result
  case class Err(reason: Status) extends Result

  trait Data {
    def data: ByteString
  }

  private def idFor(path: Option[Path]): String = path match {
    case None    => UUID.randomUUID().toString.take(8)
    case Some(p) => Base64.getEncoder.encodeToString(p.toString.getBytes)
  }

  private def sessionFor(path: Option[Path], cb: SessionCallback): api.Session = path match {
    case None =>
      val session = OmegaLib.newSessionCb(None, cb)
      session.push(List.fill(10000)(" ").mkString)
      session
    case path => OmegaLib.newSessionCb(path, cb)
  }
}

class Sessions extends Actor with ActorLogging {
  import Sessions._
  implicit val timeout = Timeout(1.second)

  def receive: Receive = {
    case Create(path) =>
      val id = idFor(path)
      context.child(id) match {
        case Some(_) =>
          sender() ! Err(Status.ALREADY_EXISTS)
        case None =>
          import context.system
          val (input, stream) = Source.queue[Session.Updated](10, OverflowStrategy.fail).preMaterialize()
          val s = sessionFor(path, _ => input.queue.offer(Session.Updated(id)))
          context.actorOf(Session.props(s, stream), id)
          sender() ! Ok(id)
      }

    case Find(id) =>
      sender() ! context.child(id)

    case Destroy(id) =>
      context.child(id) match {
        case None => sender() ! Err(Status.NOT_FOUND)
        case Some(s) =>
          s ! PoisonPill
          sender() ! Ok(id)
      }

    case SessionOp(id, op) =>
      context.child(id) match {
        case None    => sender() ! Err(Status.NOT_FOUND.withDescription("session not found"))
        case Some(s) => s forward op
      }

    case ViewportOp(sid, vid, op) =>
      val replyTo = sender()
      context.child(sid) match {
        case None =>
        case Some(s) =>
          context
            .actorSelection(s.path / vid)
            .resolveOne()
            .onComplete {
              case Success(v) => v.tell(op, replyTo)
              case Failure(_) => replyTo ! Err(Status.NOT_FOUND)
            }(context.dispatcher)
      }
  }
}
