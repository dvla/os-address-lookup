package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup._
import dvla.helpers.UnitSpec
import dvla.microservice.Configuration

final class PostcodeUrlBuilderSpec extends UnitSpec {
  "endPoint" should {
    "not specify language when none provided on the request" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode)
      val result = postcodeUrlBuilder.endPoint(request)

      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&key=test-api-key")
    }

    "specify language 'cy' when provided on the request" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = Some("cy"))
      val result = postcodeUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&lr=cy&key=test-api-key")
    }

    "specify language 'en' when provided on the request" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = Some("en"))
      val result = postcodeUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&lr=en&key=test-api-key")
    }
  }

  private final val baseUrl = "test-base-url"
  private final val postcode = "test-postcode"
  private final val apiKey = "test-api-key"
  private val configuration = Configuration(baseUrl = baseUrl, apiKey = apiKey)
  private val postcodeUrlBuilder = new PostcodeUrlBuilder(configuration)
}

