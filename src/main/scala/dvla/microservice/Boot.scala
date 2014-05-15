package dvla.microservice

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import com.typesafe.config.ConfigFactory
import akka.event.Logging
import dvla.microservice.ordnance_survey_beta_0_6.LookupCommand

object Boot extends App {

  val conf = ConfigFactory.load()

  val serverPort = conf.getInt("port")

  val osUsername = conf.getString("ordnance_survey.username")
  val osPassword = conf.getString("ordnance_survey.password")
  val osBaseUrl = conf.getString("ordnance_survey.baseurl")
  val osRequestTimeout = conf.getInt("ordnance_survey.requesttimeout")
  val configuration = Configuration(osUsername, osPassword, osBaseUrl, osRequestTimeout)

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val log = Logging(system, getClass)

  implicit val commandExecutionContext = system.dispatcher

  implicit val command = new LookupCommand(configuration)
  val creationProperties = Props(new SprayOSAddressLookupService(configuration))

  // create and start our service actor
  val service = system.actorOf(creationProperties, "micro-service")

  logStartupConfiguration

  // start ordnance_survey new HTTP server on the port specified in configuration with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "localhost", port = serverPort)

  private def logStartupConfiguration = {
    log.debug(s"Listening for HTTP on port = ${serverPort}")
    log.debug("Micro service configured to call ordnance survey web service")
    log.debug(s"Timeout = ${osRequestTimeout} millis")
  }
}