package dvla.domain

import org.scalatest.{Matchers, WordSpec}
import dvla.domain.JsonFormats._
import spray.json._
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.UprnToAddressResponse

class JsonFormatsSpec extends WordSpec with Matchers {

  "JsonFormats" should {
    "successfully unmarshall ordnance_survey valid json postcode to address lookup response payload into ordnance_survey response object" in {
      val expectedResponse = PostcodeToAddressResponse(Seq(
        UprnAddressPair("12345", s"presentationProperty stub, 789, property stub, street stub, town stub, area stub, SA11AA")))
      val jsonPayload =
        """{"addresses":[
          |{"uprn":"12345",
          |"address":"presentationProperty stub, 789, property stub, street stub, town stub, area stub, SA11AA"
          |}]}""".stripMargin
      val unmarshalledResponse = jsonPayload.parseJson.convertTo[PostcodeToAddressResponse]
      unmarshalledResponse should equal(expectedResponse)
    }

    "successfully unmarshall ordnance_survey valid json uprn to address lookup response payload into ordnance_survey response object" in {
      val expectedResponse = UprnToAddressResponse(Some(AddressViewModel(
        uprn = Some(12345),
        address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ")
      )))
      val jsonPayload =
        """{"addressViewModel":
          |{"uprn":12345,
          |"address":["44 Hythe Road","White City","London","NW10 6RJ"]
          |}}""".stripMargin
      val unmarshalledResponse = jsonPayload.parseJson.convertTo[UprnToAddressResponse]
      unmarshalledResponse should equal(expectedResponse)
    }
  }
}
