package dvla.domain

import org.scalatest.{Matchers, WordSpec}
import dvla.domain.JsonFormats._
import spray.json._
import dvla.domain.address_lookup._
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.PostcodeToAddressResponse

class JsonFormatsSpec extends WordSpec with Matchers {

  "JsonFormats" should {

    "successfully unmarshall a valid json postcode to address lookup response payload into a response object" in {
      val expectedResponse = PostcodeToAddressResponse(Seq(
        UprnAddressPair("12345", s"presentationProperty stub, 789, property stub, street stub, town stub, area stub, SA11AA")))
      val jsonPayload = """{"addresses":[{"uprn":"12345","address":"presentationProperty stub, 789, property stub, street stub, town stub, area stub, SA11AA"}]}"""
      val unmarshalledResponse = jsonPayload.asJson.convertTo[PostcodeToAddressResponse]
      unmarshalledResponse should equal(expectedResponse)
    }

    "successfully unmarshall a valid json uprn to address lookup response payload into a response object" in {
      val expectedResponse = UprnToAddressResponse(Some(AddressViewModel(uprn = Some(12345), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))))
      val jsonPayload = """{"addressViewModel":{"uprn":12345,"address":["44 Hythe Road","White City","London","NW10 6RJ"]}}"""
      val unmarshalledResponse = jsonPayload.asJson.convertTo[UprnToAddressResponse]
      unmarshalledResponse should equal(expectedResponse)
    }

  }

}