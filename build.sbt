import sbt.Scoped.Apply2
import sbtassembly.Plugin._
import AssemblyKeys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._

organization := "dvla"
name := "os-address-lookup"
version := "0.19-SNAPSHOT"
scalaVersion := "2.10.5"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val nexus = "http://rep002-01.skyscape.preview-dvla.co.uk:8081/nexus/content/repositories"

resolvers ++= Seq(
  "typesafe repo" at "http://repo.typesafe.com/typesafe/releases",
  "local nexus snapshots" at s"$nexus/snapshots",
  "local nexus releases" at s"$nexus/releases"
)

// sbt-Revolver allows the running of the spray service in sbt in the background using re-start
seq(Revolver.settings: _*)

jacoco.settings

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}}

libraryDependencies ++= {
  Seq(
    "dvla" %% "vehicles-services-common" % "0.12-SNAPSHOT",
    "io.spray" %% "spray-client" % "1.3.1",
    "ch.qos.logback" % "logback-classic" % "1.1.0",
    "org.scalatest" %% "scalatest" % "2.0" % "test",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0",
    "com.typesafe.play" %% "play-json" % "2.2.2",
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
