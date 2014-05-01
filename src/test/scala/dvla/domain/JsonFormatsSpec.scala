package dvla.domain

import org.scalatest.{Matchers, WordSpec}
import dvla.domain.JsonFormats._
import spray.json._
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest

class JsonFormatsSpec extends WordSpec with Matchers {

  "JsonFormats" should {
    "successfully unmarshall a valid json address lookup request payload into a request object" in {
      val expectedRequest = PostcodeToAddressLookupRequest("SA11AA")
      val jsonPayload = """{"postcode":"SA11AA"}"""
      val unmarshalledRequest = jsonPayload.asJson.convertTo[PostcodeToAddressLookupRequest]
      unmarshalledRequest should equal(expectedRequest)
    }

  }

  //TODO tests for other two json formats

}