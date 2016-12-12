package dvla.domain

import dvla.domain.JsonFormats._
import dvla.domain.address_lookup.AddressDto
import org.scalatest.{Matchers, WordSpec}
import spray.json._

class JsonFormatsSpec extends WordSpec with Matchers {

  "JsonFormats" should {
    "successfully unmarshall ordnance_survey valid json postcode to address lookup response payload into ordnance_survey response object" in {
      val expectedResponse = Seq(AddressDto(
        s"presentationProperty stub, 789, property stub, street stub, town stub, area stub, SA11AA",
        None,
        s"presentationProperty stub",
        Some("789, property stub"),
        Some("street stub"),
        s"town stub",
        s"SA11AA"))
      val jsonPayload =
        """[{"addressLine":"presentationProperty stub, 789, property stub, street stub, town stub, area stub, SA11AA",
          |"streetAddress1":"presentationProperty stub",
          |"streetAddress2":"789, property stub",
          |"streetAddress3":"street stub",
          |"postTown":"town stub",
          |"postCode":"SA11AA"
          |}]""".stripMargin
      val unmarshalledResponse = jsonPayload.parseJson.convertTo[Seq[AddressDto]]
      unmarshalledResponse should equal(expectedResponse)
    }
  }
}
