package example

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import omega_edit.{CreateSessionRequest, EditorClient}

object CreateSession extends App {
  implicit val system = ActorSystem("omega-grpc-client")
  import system.dispatcher

  val client = EditorClient(GrpcClientSettings.connectToServiceAt("127.0.0.1", 8080).withTls(false))

  client.getOmegaVersion(com.google.protobuf.empty.Empty()).foreach(v => println(s"v${v.major}.${v.minor}.${v.patch}"))
  client.createSession(CreateSessionRequest(None)).foreach(r => println(s"session created ${r.getSessionId.id}"))
}
