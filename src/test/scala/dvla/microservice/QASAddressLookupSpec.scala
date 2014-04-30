package dvla.microservice

import dvla.domain.JsonFormats._
import spray.http.StatusCodes._
import dvla.domain.address_lookup.PostcodeToAddressResponse

class QASAddressLookupSpec extends RouteSpecBase {

  val postcodeValid = "SA11AA"
  val addressLookupUrl = "/postcode-to-address"

  "The address lookup service" should {

    // This is used because you can't create real request object with invalid parameters
    case class TestRequest(postcode: String)

    implicit val testRequestFormat = jsonFormat1(TestRequest)

    "return a successful response containing a model for a valid address lookup request" in {
      val request = TestRequest(postcodeValid)
      Post(addressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses(0).uprn should equal ("12345")
        // TODO check address half of pair after using mocked not canned data
      }
    }

  }
}