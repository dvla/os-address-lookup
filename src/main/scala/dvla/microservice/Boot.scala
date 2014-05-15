package dvla.microservice

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http

object Boot extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  val conf = ConfigFactory.load()

  val serverPort = conf.getInt("port")
  val apiVersion = conf.getString("ordnancesurvey.apiversion")

  val configuration = {
    val osRequestTimeout = conf.getInt("ordnancesurvey.requesttimeout")

    if (apiVersion == "beta_0_6") {
      val osUsername = conf.getString("ordnancesurvey.beta06.username")
      val osPassword = conf.getString("ordnancesurvey.beta06.password")
      val osBaseUrl = conf.getString("ordnancesurvey.beta06.baseurl")
      Configuration(
        username = osUsername,
        password = osPassword,
        baseUrl = osBaseUrl)
    }
    else {
      val osBaseUrl = conf.getString("ordnancesurvey.preproduction.baseurl")
      val apiKey = conf.getString("ordnancesurvey.preproduction.apikey")
      Configuration(
        baseUrl = osBaseUrl,
        apiKey = apiKey)
    }
  }

  implicit val commandExecutionContext = system.dispatcher
  implicit val command =
    if (apiVersion == "beta_0_6") new ordnance_survey_beta_0_6.LookupCommand(configuration)
    else new ordnance_survey_preproduction.LookupCommand(configuration)


  val log = Logging(system, getClass)


  val creationProperties = Props(new SprayOSAddressLookupService(configuration))

  // create and start our service actor
  val service = system.actorOf(creationProperties, "micro-service")

  logStartupConfiguration

  // start ordnance_survey new HTTP server on the port specified in configuration with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "localhost", port = serverPort)

  private def logStartupConfiguration = {
    log.debug(s"Listening for HTTP on port = $serverPort")
    log.debug("Micro service configured to call ordnance survey web service")
  }
}