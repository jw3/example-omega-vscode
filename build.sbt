lazy val commonSettings = {
  Seq(
    name := "example-omega-server",
    version := "0.1",
    scalaVersion := "2.13.6",
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-Xfatal-warnings",
      "-Xlint:_,-byname-implicit"
    )
  )
}

lazy val commonPlugins = Seq(JavaAppPackaging, UniversalPlugin)

lazy val `example-omega-ext` = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(api, websocket, grpc, examples)

lazy val api = project
  .in(file("api"))
  .settings(commonSettings)
  .settings(
    name := "omega-api",
    libraryDependencies ++= Seq("com.github.jnr" % "jnr-ffi" % "2.2.10")
  )
  .enablePlugins(commonPlugins: _*)

lazy val examples = project
  .in(file("examples"))
  .dependsOn(api, websocket, grpc)
  .settings(commonSettings)
  .settings(
    name := "omega-examples"
  )
  .enablePlugins(commonPlugins: _*)

lazy val websocket = project
  .in(file("websocket"))
  .dependsOn(api)
  .settings(commonSettings)
  .settings(
    name := "websocket-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.32",
      "com.typesafe.akka" %% "akka-http" % "10.2.7",
      "com.typesafe.akka" %% "akka-stream" % "2.5.32",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.7",
      "io.github.paoloboni" %% "spray-json-derived-codecs" % "2.3.5"
    )
  )
  .enablePlugins(commonPlugins: _*)

lazy val grpc = project
  .in(file("grpc"))
  .dependsOn(api)
  .settings(commonSettings)
  .settings(
    name := "grpc-backend",
    libraryDependencies ++= Seq()
  )
  .enablePlugins(commonPlugins: _*)
  .enablePlugins(AkkaGrpcPlugin)
