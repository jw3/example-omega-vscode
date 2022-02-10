package omega.grpc.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.grpc.GrpcServiceException
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.google.protobuf.empty.Empty
import io.grpc.Status
import omega.grpc.server.EditorService._
import omega.grpc.server.Session.{DestroyView, Save, View}
import omega.grpc.server.Sessions._
import omega.grpc.server.Viewport.Events
import omega_edit.ChangeKind.{CHANGE_DELETE, CHANGE_INSERT, CHANGE_OVERWRITE}
import omega_edit._

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class EditorService(implicit val system: ActorSystem, implicit val mat: Materializer) extends Editor {
  private implicit val timeout: Timeout = Timeout(1.second)
  private val sessions = system.actorOf(Sessions.props())
  import system.dispatcher

  def getOmegaVersion(in: Empty): Future[VersionResponse] = {
    val v = OmegaLib.version()
    Future.successful(VersionResponse(v.major, v.minor, v.patch))
  }

  def createSession(in: CreateSessionRequest): Future[CreateSessionResponse] =
    (sessions ? Create(in.filePath.map(Paths.get(_)))).mapTo[Result].map {
      case Ok(id) => CreateSessionResponse(Some(ObjectId(id)))
      case Err(c) => throw grpcFailure(c)
    }

  def destroySession(in: ObjectId): Future[ObjectId] =
    (sessions ? Destroy(in.id)).mapTo[Result].map {
      case Ok(_)  => in
      case Err(c) => throw grpcFailure(c)
    }

  def saveSession(in: SaveSessionRequest): Future[SaveSessionResponse] =
    in.sessionId.map(_.id) match {
      case None => grpcFailFut(Status.INVALID_ARGUMENT, "path required")
      case Some(id) =>
        (sessions ? SessionOp(id, Save(Paths.get(in.filePath)))).mapTo[Result].map {
          case Ok(id) => SaveSessionResponse(Some(ObjectId(id)))
          case Err(c) => throw grpcFailure(c)
        }
    }

  def createViewport(in: CreateViewportRequest): Future[CreateViewportResponse] = in.sessionId match {
    case None => grpcFailFut(Status.INVALID_ARGUMENT, "session id required")
    case Some(oid) =>
      (sessions ? SessionOp(oid.id, View(in.offset, in.capacity))).mapTo[Result].map {
        case Ok(id) => CreateViewportResponse(Some(ObjectId(id)))
        case Err(c) => throw grpcFailure(c)
      }
  }

  def destroyViewport(in: ObjectId): Future[ObjectId] =
    in match {
      case Viewport.Id(sid, vid) =>
        (sessions ? SessionOp(sid, DestroyView(vid))).mapTo[Result].map {
          case Ok(_)  => in
          case Err(c) => throw grpcFailure(c)
        }
      case _ => grpcFailFut(Status.INVALID_ARGUMENT, "malformed viewport id")
    }

  def getViewportData(in: ViewportDataRequest): Future[ViewportDataResponse] = in.viewportId match {
    case Some(Viewport.Id(sid, vid)) =>
      (sessions ? ViewportOp(sid, vid, Viewport.Get)).mapTo[Result].map {
        case Err(c)           => throw grpcFailure(c)
        case ok: Ok with Data => ViewportDataResponse(Some(ObjectId(ok.id)), ok.data.size(), ok.data)
        case Ok(id)           => ViewportDataResponse(Some(ObjectId(id)))
      }
    case None    => grpcFailFut(Status.INVALID_ARGUMENT, "viewport id required")
    case Some(_) => grpcFailFut(Status.INVALID_ARGUMENT, "malformed viewport id")
  }

  def submitChange(in: ChangeRequest): Future[ChangeResponse] = in.sessionId match {
    case None => grpcFailFut(Status.INVALID_ARGUMENT, "session id required")
    case Some(oid) =>
      opForRequest(in) match {
        case None => grpcFailFut(Status.INVALID_ARGUMENT, "undefined change kind")
        case Some(op) =>
          (sessions ? SessionOp(oid.id, op)).mapTo[Result].map {
            case Ok(id) => ChangeResponse(Some(ObjectId(id)))
            case Err(c) => throw grpcFailure(c)
          }
      }
  }

  def getChangeDetails(in: SessionChange): Future[ChangeDetailsResponse] = ???

  def unsubscribeOnChangeSession(in: ObjectId): Future[ObjectId] = ???

  def unsubscribeOnChangeViewport(in: ObjectId): Future[ObjectId] = ???

  /**
    * Event streams
    */
  def subscribeOnChangeSession(in: ObjectId): Source[SessionChange, NotUsed] = ???

  def subscribeOnChangeViewport(in: ObjectId): Source[ViewportChange, NotUsed] = in match {
    case Viewport.Id(sid, vid) =>
      val f = (sessions ? ViewportOp(sid, vid, Viewport.Watch)).mapTo[Result].map {
        case ok: Ok with Events => ok.stream.map(u => ViewportChange(Some(ObjectId(u.id))))
        case _                  => Source.empty
      }
      Await.result(f, 1.second)
    case _ => Source.failed(new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("malformed viewport id")))
  }
}

object EditorService {
  def grpcFailure(status: Status, message: String = ""): GrpcServiceException =
    new GrpcServiceException(if (message.nonEmpty) status.withDescription(message) else status)

  def grpcFailFut[T](status: Status, message: String = ""): Future[T] =
    Future.failed(grpcFailure(status, message))

  def opForRequest(in: ChangeRequest): Option[Session.Op] = in.data.map(_.toStringUtf8).flatMap { data =>
    in.kind match {
      case CHANGE_INSERT    => Some(Session.Insert(data, in.offset))
      case CHANGE_DELETE    => Some(Session.Delete(in.offset, in.length))
      case CHANGE_OVERWRITE => Some(Session.Overwrite(data, in.offset))
      case _                => None
    }
  }
}
