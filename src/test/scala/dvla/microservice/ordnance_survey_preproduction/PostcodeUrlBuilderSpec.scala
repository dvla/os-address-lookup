package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup._
import dvla.helpers.UnitSpec
import dvla.microservice.Configuration

final class PostcodeUrlBuilderSpec extends UnitSpec {
  "endPoint" should {
    "not specify language when none provided on the request" in {
      val configuration = Configuration(baseUrl = baseUrl, apiKey = apiKey)
      val request = PostcodeToAddressLookupRequest(postcode = postcode)
      val result = new PostcodeUrlBuilder(configuration).endPoint(request)

      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&key=test-api-key")
    }
  }

  private final val baseUrl = "test-base-url"
  private final val postcode = "test-postcode"
  private final val apiKey = "test-api-key"
}

