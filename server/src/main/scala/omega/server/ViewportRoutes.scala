package omega.server

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import omega.server.protocol._
import spray.json._

import scala.util.{Failure, Success, Try}

object ViewportRoutes {
  def make(session: ActorRef)(implicit mat: Materializer): Route =
    path("view") {
      get {
        extractWebSocketUpgrade { upgrade =>
          complete {
            val (ws, source) = Source
              .actorRef[ViewUpdated](bufferSize = 100, overflowStrategy = OverflowStrategy.dropHead)
              .map(v => TextMessage(v.toJson.compactPrint))
              .preMaterialize()
            upgrade.handleMessages(
              Flow.fromSinkAndSource(
                Sink.foreach {
                  case TextMessage.Strict(txt) =>
                    Try(txt.trim.parseJson.convertTo[SessionOp]) match {
                      case Success(v @ View(_, _, _)) => session ! ConnectView(ws, v)
                      case Success(op)                => session ! op
                      case Failure(e)                 => println(s"deser fail: $txt $e")
                    }
                  case _ => println("wasnt strict")
                },
                source
              )
            )
          }
        }
      }
    }
}
