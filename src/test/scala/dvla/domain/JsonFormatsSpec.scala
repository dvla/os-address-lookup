package dvla.domain

import org.scalatest.{Matchers, WordSpec}
import dvla.domain.JsonFormats._
import spray.json._
import dvla.domain.address_lookup.{UprnToAddressLookupRequest, PostcodeToAddressLookupRequest}

class JsonFormatsSpec extends WordSpec with Matchers {

  "JsonFormats" should {

    "successfully unmarshall a valid json postocde to address lookup request payload into a request object" in {
      val expectedRequest = PostcodeToAddressLookupRequest("SA11AA")
      val jsonPayload = """{"postcode":"SA11AA"}"""
      val unmarshalledRequest = jsonPayload.asJson.convertTo[PostcodeToAddressLookupRequest]
      unmarshalledRequest should equal(expectedRequest)
    }

    "successfully unmarshall a valid json uprn to address lookup request payload into a request object" in {
      val expectedRequest = UprnToAddressLookupRequest(12345L)
      val jsonPayload = """{"uprn":12345}"""
      val unmarshalledRequest = jsonPayload.asJson.convertTo[UprnToAddressLookupRequest]
      unmarshalledRequest should equal(expectedRequest)
    }

  }

  //TODO tests for other json formats

}