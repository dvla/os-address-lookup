package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup._
import dvla.helpers.UnitSpec
import dvla.microservice.Configuration

final class UprnUrlBuilderSpec extends UnitSpec {
  "endPoint" should {
    "not specify language when none provided on the request" in {
      val request = UprnToAddressLookupRequest(uprn = uprn)
      val result = uprnUrlBuilder.endPoint(request)

      result should equal(s"test-base-url/uprn?uprn=12345&dataset=dpa&key=test-api-key")
    }

    "specify language 'cy' when provided on the request" in {
      val request = UprnToAddressLookupRequest(uprn = uprn, languageCode = Some("cy"))
      val result = uprnUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/uprn?uprn=12345&dataset=dpa&lr=cy&key=test-api-key")
    }

    "specify language 'en' when provided on the request" in {
      val request = UprnToAddressLookupRequest(uprn = uprn, languageCode = Some("en"))
      val result = uprnUrlBuilder.endPoint(request)
      result should equal(s"test-base-url/uprn?uprn=12345&dataset=dpa&lr=en&key=test-api-key")
    }
  }

  private final val baseUrl = "test-base-url"
  private final val uprn = 12345L
  private final val apiKey = "test-api-key"
  private val configuration = Configuration(baseUrl = baseUrl, apiKey = apiKey)
  private val uprnUrlBuilder = new UprnUrlBuilder(configuration)
}

