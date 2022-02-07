package omega.server

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import omega.server.protocol._

object ViewportRoutes {
  def make(session: ActorRef): Route =
    path("view" / IntNumber / IntNumber) { (o, l) =>
      get {
        extractWebSocketUpgrade { upgrade =>
          complete {
            val source = Source
              .actorRef[ViewUpdated](bufferSize = 0, overflowStrategy = OverflowStrategy.fail)
              .map(v => TextMessage(v.data))
              .mapMaterializedValue(ref => session ! ConnectView(ref, o, l))
            upgrade.handleMessages(Flow.fromSinkAndSource(Sink.ignore, source))
          }
        }
      }
    }
}
