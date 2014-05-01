package dvla.microservice

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import com.typesafe.config.ConfigFactory
import akka.event.Logging

object Boot extends App {

  val conf = ConfigFactory.load()

  val serverPort = conf.getInt("port")

  val osUsername = conf.getString("ordnancesurvey.username")
  val osPassword = conf.getString("ordnancesurvey.password")
  val osBaseUrl = conf.getString("ordnancesurvey.baseurl")
  val osRequestTimeout = conf.getInt("ordnancesurvey.requesttimeout")
  val configuration = Configuration(osUsername, osPassword, osBaseUrl, osRequestTimeout)

  val callOSWebService = conf.getBoolean("callOSWebService")


  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val log = Logging(system, getClass)

  implicit val commandExecutionContext = system.dispatcher

  // TODO: add soap implementation once we have the wsdl
  implicit val command = if (callOSWebService) new OSAddressLookupCommand(configuration) else new CannedAddressLookupCommand(configuration)
  val creationProperties = Props(new SprayOSAddressLookupService(configuration))

  // create and start our service actor
  val service = system.actorOf(creationProperties, "micro-service")

  logStartupConfiguration

  // start a new HTTP server on the port specified in configuration with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "localhost", port = serverPort)

  private def logStartupConfiguration = {
    log.debug(s"Listening for HTTP on port = ${serverPort}")
    callOSWebService match {
      case true  =>
        log.debug("Micro service configured to call ordnance survey web service")
        log.debug(s"Timeout = ${osRequestTimeout} millis")
      case false => log.debug("Micro service configured to return canned data")
    }
  }
}