package omega.grpc.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.grpc.GrpcServiceException
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.ctc.omega_edit.api.OmegaEdit
import com.google.protobuf.empty.Empty
import io.grpc.Status
import omega.grpc.server.EditorService._
import omega.grpc.server.Session._
import omega.grpc.server.Sessions._
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
    val v = OmegaEdit.version()
    Future.successful(VersionResponse(v.major, v.minor, v.patch))
  }

  def createSession(in: CreateSessionRequest): Future[CreateSessionResponse] =
    (sessions ? Create(in.sessionIdDesired, in.filePath.map(Paths.get(_)))).mapTo[Result].map {
      case Ok(id) => CreateSessionResponse(id)
      case Err(c) => throw grpcFailure(c)
    }

  def destroySession(in: ObjectId): Future[ObjectId] =
    (sessions ? Destroy(in.id)).mapTo[Result].map {
      case Ok(_)  => in
      case Err(c) => throw grpcFailure(c)
    }

  def saveSession(in: SaveSessionRequest): Future[SaveSessionResponse] =
    (sessions ? SessionOp(in.sessionId, Save(Paths.get(in.filePath)))).mapTo[Result].map {
      case Ok(id) => SaveSessionResponse(id)
      case Err(c) => throw grpcFailure(c)
    }

  def createViewport(in: CreateViewportRequest): Future[CreateViewportResponse] =
    (sessions ? SessionOp(in.sessionId, View(in.offset, in.capacity, in.viewportIdDesired))).mapTo[Result].map {
      case Ok(id) => CreateViewportResponse(id)
      case Err(c) => throw grpcFailure(c)
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

  def getViewportData(in: ViewportDataRequest): Future[ViewportDataResponse] = ObjectId(in.viewportId) match {
    case Viewport.Id(sid, vid) =>
      (sessions ? ViewportOp(sid, vid, Viewport.Get)).mapTo[Result].map {
        case Err(c)           => throw grpcFailure(c)
        case ok: Ok with Data => ViewportDataResponse(ok.id, ok.data.size(), ok.data)
        case Ok(id)           => ViewportDataResponse(id)
      }
    case _ => grpcFailFut(Status.INVALID_ARGUMENT, "malformed viewport id")
  }

  def submitChange(in: ChangeRequest): Future[ChangeResponse] =
    opForRequest(in) match {
      case None => grpcFailFut(Status.INVALID_ARGUMENT, "undefined change kind")
      case Some(op) =>
        (sessions ? SessionOp(in.sessionId, op)).mapTo[Result].map {
          case Ok(id) => ChangeResponse(id)
          case Err(c) => throw grpcFailure(c)
        }
    }

  def getChangeDetails(in: SessionEvent): Future[ChangeDetailsResponse] = in.serial match {
    case None => grpcFailFut(Status.INVALID_ARGUMENT, "change serial id required")
    case Some(cid) =>
      (sessions ? SessionOp(in.sessionId, LookupChange(cid))).mapTo[Result].map {
        case ok: Ok with ChangeDetails =>
          ChangeDetailsResponse(in.sessionId, cid, offset = ok.change.offset, length = ok.change.length)
        case Ok(_)  => ChangeDetailsResponse(in.sessionId)
        case Err(c) => throw grpcFailure(c)
      }
  }

  def unsubscribeToSessionEvents(in: ObjectId): Future[ObjectId] = Future.successful(in)
  def unsubscribeToViewportEvents(in: ObjectId): Future[ObjectId] = Future.successful(in)

  /**
    * Event streams
    */
  def subscribeToSessionEvents(in: ObjectId): Source[SessionEvent, NotUsed] = {
    val f = (sessions ? SessionOp(in.id, Session.Watch)).mapTo[Result].map {
      case ok: Ok with Session.Events =>
        ok.stream.map(u => SessionEvent(u.id))
      case _ => Source.failed(grpcFailure(Status.UNKNOWN))
    }
    Await.result(f, 1.second)
  }

  def subscribeToViewportEvents(in: ObjectId): Source[ViewportEvent, NotUsed] = in match {
    case Viewport.Id(sid, vid) =>
      val f = (sessions ? ViewportOp(sid, vid, Viewport.Watch)).mapTo[Result].map {
        case ok: Ok with Viewport.Events =>
          ok.stream.map(u => ViewportEvent(u.id, serial = u.change.map(_.id)))
        case _ => Source.failed(grpcFailure(Status.UNKNOWN))
      }
      Await.result(f, 1.second)
    case _ => Source.failed(new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("malformed viewport id")))
  }

  def undoLastChange(in: ObjectId): Future[ChangeResponse] = grpcFailFut(Status.UNIMPLEMENTED)

  def redoLastUndo(in: ObjectId): Future[ChangeResponse] = grpcFailFut(Status.UNIMPLEMENTED)

  def clearChanges(in: ObjectId): Future[ObjectId] = grpcFailFut(Status.UNIMPLEMENTED)

  def pauseViewportEvents(in: ObjectId): Future[ObjectId] = grpcFailFut(Status.UNIMPLEMENTED)

  def resumeViewportEvents(in: ObjectId): Future[ObjectId] = grpcFailFut(Status.UNIMPLEMENTED)

  def getLastChange(in: ObjectId): Future[ChangeDetailsResponse] = grpcFailFut(Status.UNIMPLEMENTED)

  def getLastUndo(in: ObjectId): Future[ChangeDetailsResponse] = grpcFailFut(Status.UNIMPLEMENTED)

  def getComputedFileSize(in: ObjectId): Future[ComputedFileSizeResponse] = grpcFailFut(Status.UNIMPLEMENTED)
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

  def bind(iface: String = "127.0.0.1", port: Int = 9000)(implicit system: ActorSystem): Future[Http.ServerBinding] =
    Http().newServerAt(iface, port).bind(EditorHandler(new EditorService))
}
