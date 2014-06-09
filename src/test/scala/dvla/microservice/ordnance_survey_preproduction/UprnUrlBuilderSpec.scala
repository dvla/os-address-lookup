package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup._
import dvla.helpers.UnitSpec
import dvla.microservice.Configuration

final class UprnUrlBuilderSpec extends UnitSpec {
  "endPoint" should {
    "not specify language when none provided on the request" in {
      val configuration = Configuration(baseUrl = baseUrl, apiKey = apiKey)
      val request = UprnToAddressLookupRequest(uprn = uprn)
      val result = new UprnUrlBuilder(configuration).endPoint(request)

      result should equal(s"test-base-url/uprn?uprn=12345&dataset=dpa&key=test-api-key")
    }
  }

  private final val baseUrl = "test-base-url"
  private final val uprn = 12345L
  private final val apiKey = "test-api-key"
}

