package omega.grpc

import akka.actor.ActorSystem
import omega.grpc.server.EditorService

import scala.concurrent.ExecutionContext

object boot extends App {
  implicit val sys: ActorSystem = ActorSystem("omega-grpc")
  implicit val ec: ExecutionContext = sys.dispatcher

  EditorService.bind().foreach { binding =>
    println(s"gRPC server bound to: ${binding.localAddress}")
  }
}
