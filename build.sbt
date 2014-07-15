import sbtassembly.Plugin._
import AssemblyKeys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._

organization := "dvla"

version := "0.1"

scalaVersion := "2.10.3"

val nexus = "http://rep002-01.skyscape.preview-dvla.co.uk:8081/nexus/content/repositories"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "typesafe repo" at "http://repo.typesafe.com/typesafe/releases",
  "local nexus snapshots" at s"$nexus/snapshots",
  "local nexus releases" at s"$nexus/releases"
)

// sbt-Revolver allows the running of the spray service in sbt in the background using re-start
seq(Revolver.settings: _*)

jacoco.settings

libraryDependencies ++= {
  val akkaV = "2.3.3"
  val sprayV = "1.3.1"
  Seq(
    "dvla" % "vehicles-services-common_2.10" % "0.2-SNAPSHOT",
    "io.spray" % "spray-can" % sprayV,
    "io.spray" % "spray-client" % sprayV,
    "io.spray" % "spray-caching" % sprayV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "io.spray" % "spray-routing" % sprayV,
    "io.spray" % "spray-testkit" % sprayV,
    "io.spray" %% "spray-json" % "1.2.5",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "ch.qos.logback" % "logback-classic" % "1.1.0",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0",
    "com.typesafe.play" %% "play-json" % "2.2.2",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test"
  )
}
credentials += Credentials(Path.userHome / ".sbt/.credentials")

publishTo := {
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at s"$nexus/snapshots")
  else
    Some("releases"  at s"$nexus/releases")
}
