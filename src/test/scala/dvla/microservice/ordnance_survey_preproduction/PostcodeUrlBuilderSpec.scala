package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup._
import dvla.helpers.UnitSpec
import dvla.microservice.Configuration

class PostcodeUrlBuilderSpec extends UnitSpec {
  "endPoint" should {
    "not specify language filter when none provided on the request" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode)
      val result = postcodeUrlBuilder.endPoint(request)

      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&key=test-api-key")
    }

    "specify language filter 'cy' when provided on the request" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = Some("cy"))
      val result = postcodeUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&lr=CY&key=test-api-key")
    }

    "specify language filter 'en' when provided on the request" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = Some("en"))
      val result = postcodeUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&lr=EN&key=test-api-key")
    }

    "remove regional codes e.g. 'en-us' -> 'EN'" in {
      val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = Some("en-us"))
      val result = postcodeUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/postcode?postcode=test-postcode&dataset=dpa&lr=EN&key=test-api-key")
    }
  }

  private final val baseUrl = "test-base-url"
  private final val postcode = "test-postcode"
  private final val apiKey = "test-api-key"
  private val configuration = Configuration(baseUrl = baseUrl, apiKey = apiKey)
  private val postcodeUrlBuilder = new PostcodeUrlBuilder(configuration)
}

