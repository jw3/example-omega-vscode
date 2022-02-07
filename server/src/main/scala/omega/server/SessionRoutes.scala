package omega.server

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import omega.server.protocol._

object SessionRoutes {
  def make(session: ActorRef): Route =
    path("op") {
      post {
        entity(as[SessionOp]) { e =>
          session ! e
          complete(StatusCodes.Accepted)
        }
      }
    }
}
