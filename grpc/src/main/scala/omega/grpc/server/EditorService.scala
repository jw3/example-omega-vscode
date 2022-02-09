package omega.grpc.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.google.protobuf.empty.Empty
import omega_edit._

import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.Future

class EditorService(implicit val system: ActorSystem, implicit val mat: Materializer) extends Editor {
  def getOmegaVersion(in: Empty): Future[VersionResponse] = {
    val v = OmegaLib.version()
    Future.successful(VersionResponse(v.major, v.minor, v.patch))
  }

  def createSession(in: CreateSessionRequest): Future[CreateSessionResponse] = {
    val id = UUID.randomUUID().toString
    system.actorOf(Session.props(in.filePath.map(Paths.get(_))), id)
    Future.successful(CreateSessionResponse(Some(ObjectId(id))))
  }

  def saveSession(in: SaveSessionRequest): Future[SaveSessionResponse] = ???

  def destroySession(in: ObjectId): Future[ObjectId] = ???

  def submitChange(in: ChangeRequest): Future[ChangeResponse] = ???

  def createViewport(in: CreateViewportRequest): Future[CreateViewportResponse] = ???

  def getViewportData(in: ViewportDataRequest): Future[ViewportDataResponse] = ???

  def destroyViewport(in: ObjectId): Future[ObjectId] = ???

  def getChangeDetails(in: SessionChange): Future[ChangeDetailsResponse] = ???

  def unsubscribeOnChangeSession(in: ObjectId): Future[ObjectId] = ???

  def unsubscribeOnChangeViewport(in: ObjectId): Future[ObjectId] = ???

  /**
    * Event streams
    */
  def subscribeOnChangeSession(in: ObjectId): Source[SessionChange, NotUsed] = ???

  def subscribeOnChangeViewport(in: ObjectId): Source[ViewportChange, NotUsed] = ???
}
