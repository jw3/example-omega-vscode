package omega.websocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import omega.websocket.server.{Session, SessionRoutes, ViewportRoutes}

import java.nio.file.Paths

object boot extends App {
  implicit val system = ActorSystem("omega-websocket")
  implicit val mat = ActorMaterializer()

  val datafile = args.headOption.map(Paths.get(_))
  datafile.foreach(path => require(path.toFile.exists(), s"$path does not exist"))

  val session = system.actorOf(Session.props(datafile))
  val api = pathPrefix("api") {
    SessionRoutes.make(session) ~ ViewportRoutes.make(session)
  }
  Http().newServerAt("127.0.0.1", 9000).bindFlow(api)
}
