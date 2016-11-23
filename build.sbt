
organization := "dvla"
name := "os-address-lookup"
version := "0.32-SNAPSHOT"
scalaVersion := "2.11.8"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val nexus = "http://rep002-01.skyscape.preview-dvla.co.uk:8081/nexus/content/repositories"

resolvers ++= Seq(
  "typesafe repo" at "http://repo.typesafe.com/typesafe/releases",
  "local nexus snapshots" at s"$nexus/snapshots",
  "local nexus releases" at s"$nexus/releases"
)

// sbt-Revolver allows the running of the spray service in sbt in the background using re-start
Seq(Revolver.settings: _*)

test in assembly := {}

libraryDependencies ++= {
  Seq(
    "dvla" %% "vehicles-services-common" % "0.17-SNAPSHOT",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.play" %% "play-json" % "2.3.10",
    "io.spray" %% "spray-client" % "1.3.2",
    //test
    "io.spray" %% "spray-testkit" % "1.3.2" % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.1" % "test",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )
}

credentials += Credentials(Path.userHome / ".sbt/.credentials")

publishTo := {
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at s"$nexus/snapshots")
  else
    Some("releases"  at s"$nexus/releases")
}
