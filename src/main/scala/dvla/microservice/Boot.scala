package dvla.microservice

import java.util.TimeZone

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import com.typesafe.config.ConfigFactory
import dvla.microservice.ordnance_survey_preproduction.{CallOrdnanceSurveyImpl, PostcodeUrlBuilder}
import org.joda.time.DateTimeZone
import spray.can.Http

object Boot extends App {
  setDefaultTimeZone()

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  private val conf = ConfigFactory.load()

  private val service = {
    val configuration = {
      val osBaseUrl = conf.getString("ordnancesurvey.preproduction.baseurl")
      val apiKey = conf.getString("ordnancesurvey.preproduction.apikey")
      Configuration(
        baseUrl = osBaseUrl,
        apiKey = apiKey)
    }

    implicit val commandExecutionContext = system.dispatcher

    val command = {
      val callOrdnanceSurvey = {
        val postcodeUrlBuilder = new PostcodeUrlBuilder(configuration = configuration)
        new CallOrdnanceSurveyImpl(postcodeUrlBuilder)
      }
      new ordnance_survey_preproduction.LookupCommand(
        configuration = configuration,
        callOrdnanceSurvey = callOrdnanceSurvey
      )
    }

    val creationProperties = Props(new SprayOSAddressLookupService(configuration, command))

    system.actorOf(creationProperties, "micro-service") // create and start our service actor
  }

  private val port = conf.getInt("port")

  private val address = conf.getString("address")

  private val log = Logging(system, getClass)
  logStartupConfiguration()

  // start ordnance_survey new HTTP server on the port specified in configuration with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = address, port = port)

  private def logStartupConfiguration() = {
    log.info(s"Listening for HTTP on $address:$port")
    val url = conf.getString("ordnancesurvey.preproduction.baseurl")
    log.info(s"Micro service configured to call ordnance survey web service on url $url")
  }

  private def setDefaultTimeZone() = {
    val localTimeZone = "UTC"
    TimeZone.setDefault(TimeZone.getTimeZone(localTimeZone))
    DateTimeZone.setDefault(DateTimeZone.forID(localTimeZone))
  }
}