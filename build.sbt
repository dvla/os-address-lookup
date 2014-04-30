import sbtassembly.Plugin._
import AssemblyKeys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._

organization := "dvla"

version := "0.1"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)

// sbt-Revolver allows the running of the spray service in sbt in the background using re-start
seq(Revolver.settings: _*)

// ------ Start: CFX settings for importing WSDL using the apache cfx plugin
seq(cxf.settings :_*)

// DEVELOPER NOTE: if you make any changes to this file, in order for sbt to pick them up you must
// either stop and start sbt or run the "reload" command from inside the sbt console

cxf.wsdls := Seq(
// TODO: Once we have wsdl for the vehicle lookup add an entry here for it
//  cxf.Wsdl(file("src/main/business_service/v1/DisposeToTradeService.wsdl"), Seq("-asyncMethods", "-quiet", "-mark-generated", ""), "business_service-disposetotradeservice")
)

cxf.cxfVersion := "2.7.10"

sourceGenerators in Compile <+= cxf.wsdl2java in Compile

// jdk7u25 removed the stack search for a resource bundle for Logger to use
// The workaround to enable the stack walk search is the below system property
fork in run := true

javaOptions in (run) += "-Djdk.logging.allowStackWalkSearch=true"

// ------ End: jdk workaround settings

jacoco.settings

// TODO: This is currently here as the cxf source generator is creating a soap client with a Main, but should not
mainClass in (Compile,run) := Some("dvla.microservice.Boot")

packageOptions in (Compile, packageBin) +=
  Package.ManifestAttributes( java.util.jar.Attributes.Name.MAIN_CLASS -> "dvla.microservice.Boot" )

// Specify the merge strategy for the files that the assembly plugin reports
// a deduplicate: different file contents found in the following... exception.
// This relates to the cxf dependencies.
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
  case "META-INF/cxf/bus-extensions.txt" => MergeStrategy.first
  case "META-INF/tools-plugin.xml" => MergeStrategy.first
  case x => old(x) } }

// ------ End: CFX settings

libraryDependencies ++= {
  val akkaV = "2.2.3"
  val sprayV = "1.2.0"
  Seq(
    "io.spray" % "spray-can" % sprayV,
    "io.spray" % "spray-caching" % sprayV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "io.spray" % "spray-routing" % sprayV,
    "io.spray" % "spray-testkit" % sprayV,
    "io.spray" %% "spray-json" % "1.2.5",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "ch.qos.logback" % "logback-classic" % "1.1.0",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0",
    "org.apache.cxf" % "cxf-tools-wsdlto-frontend-jaxws" % cxf.cxfVersion.value,
    "org.apache.cxf" % "cxf-tools-wsdlto-databinding-jaxb" % cxf.cxfVersion.value
  )
}
