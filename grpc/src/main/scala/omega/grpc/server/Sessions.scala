package omega.grpc.server

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.util.Timeout
import com.google.protobuf.ByteString
import io.grpc.Status

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
          context.actorOf(Session.props(path), id)
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
          val sel = context.actorSelection(s.path / vid)
          sel
            .resolveOne()
            .onComplete {
              case Success(v) => v.tell(op, replyTo)
              case Failure(_) => replyTo ! Err(Status.NOT_FOUND)
            }(context.dispatcher)
      }
  }
}
