package dvla.microservice

import com.typesafe.config.{ConfigFactory, Config}
import dvla.domain.address_lookup.{UprnToAddressLookupRequest, PostcodeToAddressLookupRequest}
import dvla.helpers.UnitSpec
import dvla.microservice.ordnance_survey_preproduction._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global

class LookupCommandIntegrationSpec extends UnitSpec{

  private val validUprn = 10010030533L
  private val validPostcode = "SA11AA"

  private val conf = ConfigFactory.load()
  private val configuration = new Configuration(
    baseUrl = conf.getString("ordnancesurvey.override.baseurl"),
    apiKey = conf.getString("ordnancesurvey.preproduction.apikey"))

  "call os address lookup real endpoint" should {

    def testConfig: Config = {
      ConfigFactory.empty().withFallback(ConfigFactory.load())
    }

    implicit val system = ActorSystem("LookupCommandSpecPreProduction", testConfig)

    val command = {
      val callOrdnanceSurvey = {
        val postcodeUrlBuilder = new PostcodeUrlBuilder(configuration = configuration)
        val uprnUrlBuilder = new UprnUrlBuilder(configuration = configuration)
        new CallOrdnanceSurveyImpl(postcodeUrlBuilder, uprnUrlBuilder)
      }
      new ordnance_survey_preproduction.LookupCommand(
        configuration = configuration,
        callOrdnanceSurvey = callOrdnanceSurvey
      )
    }

    "return a valid postcode response" in {
      val result = command(PostcodeToAddressLookupRequest(validPostcode))

      whenReady(result, Timeout(Span(10, Seconds))) { r =>
        r.addresses.foreach(a => a.uprn should equal(validUprn.toString))
      }
    }
    "return a valid uprn response" in {
      val result = command(UprnToAddressLookupRequest(validUprn))

      whenReady(result, Timeout(Span(10, Seconds))) { r =>
        r.addressViewModel.foreach(a => a.uprn.getOrElse(0) should equal(validUprn))
      }
    }
  }

}
