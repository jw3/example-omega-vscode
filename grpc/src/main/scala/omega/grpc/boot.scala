package omega.grpc

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import omega.grpc.server.EditorService
import omega_edit.EditorHandler

import scala.concurrent.{ExecutionContext, Future}

object boot extends App {
  implicit val sys: ActorSystem = ActorSystem("omega-grpc")
  implicit val ec: ExecutionContext = sys.dispatcher

  val service: HttpRequest => Future[HttpResponse] = EditorHandler(new EditorService())

  val binding = Http().newServerAt("127.0.0.1", 8080).bind(service)
  binding.foreach { binding =>
    println(s"gRPC server bound to: ${binding.localAddress}")
  }
}
