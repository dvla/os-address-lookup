package dvla.microservice

import com.typesafe.config.{ConfigFactory, Config}
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.helpers.UnitSpec
import dvla.microservice.ordnance_survey_preproduction._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global

class LookupCommandIntegrationSpec extends UnitSpec {

  private val validUprn = 10033544614L
  private val validPostcode = "SW1A1AA"

  private val conf = ConfigFactory.load()
  private def configuration = new Configuration(
    baseUrl = conf.getString("ordnancesurvey.override.baseurl"),
    apiKey = conf.getString("ordnancesurvey.preproduction.apikey"))

  "call os address lookup using real endpoint" should {
    def testConfig: Config = {
      ConfigFactory.empty().withFallback(ConfigFactory.load())
    }

    implicit val system = ActorSystem("LookupCommandSpecPreProduction", testConfig)
    implicit val trackingId = TrackingId("test-tracking-id")

    def command = {
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

    "return a valid postcode response for Buckingham Palace" ignore {
      val result = command(PostcodeToAddressLookupRequest(validPostcode))

      whenReady(result, Timeout(Span(10, Seconds))) { r =>
        r.addresses.foreach(a => a.uprn should equal(validUprn.toString))
      }
    }
  }
}