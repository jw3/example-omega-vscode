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
  .aggregate(grpc, examples)

lazy val examples = project
  .in(file("examples"))
  .dependsOn(grpc)
  .settings(commonSettings)
  .settings(
    name := "omega-examples"
  )
  .enablePlugins(commonPlugins: _*)

val omegaEditVersion = "0.7.0-22-g96a9db5"
lazy val grpc = project
  .in(file("grpc"))
  .settings(commonSettings)
  .settings(
    name := "grpc-backend",
    libraryDependencies ++= Seq(
      "com.ctc" %% "omega-edit" % omegaEditVersion,
      "com.ctc" %% "omega-edit-native" % omegaEditVersion classifier s"$arch"
    ),
    resolvers += Resolver.mavenLocal
  )
  .enablePlugins(commonPlugins: _*)
  .enablePlugins(AkkaGrpcPlugin)

lazy val arch: String = {
  val Mac = """mac.+""".r
  val Amd = """amd(\d+)""".r
  val x86 = """x86_(\d+)""".r

  val os = System.getProperty("os.name").toLowerCase match {
    case "linux"   => "linux"
    case Mac()     => "osx"
    case "windows" => "windows"
  }

  val arch = System.getProperty("os.arch").toLowerCase match {
    case Amd(bits) => bits
    case x86(bits) => bits
    case arch      => throw new IllegalStateException(s"unknown arch: $arch")
  }
  s"$os-$arch"
}
