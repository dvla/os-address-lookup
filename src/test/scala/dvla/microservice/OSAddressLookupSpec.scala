package dvla.microservice

import dvla.domain.JsonFormats._
import spray.http.StatusCodes._
import dvla.domain.address_lookup.{AddressViewModel, UprnToAddressResponse, PostcodeToAddressResponse}

class OSAddressLookupSpec extends RouteSpecBase {

  val postcodeValid = "SA11AA"
  val uprnValid = 12345L
  val postocdeToAddressLookupUrl = "/postcode-to-address"
  val uprntoAddressLookupUrl = "/uprn-to-address"


  private def testValidAddressViewModel(addressViewModel: AddressViewModel) = {
    addressViewModel.uprn.get should equal(12345)
    //TODO test address parts of the the model
  }


  "The postcode to address lookup service" should {

    // This is used because you can't create real request object with invalid parameters
    case class TestRequest(postcode: String)

    implicit val testRequestFormat = jsonFormat1(TestRequest)

    "return a successful response containing a model for a valid postcode to address lookup request" in {
      val request = TestRequest(postcodeValid)
      Post(postocdeToAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses(0).uprn should equal("12345")
        // TODO check address half of pair after using mocked not canned data
      }
    }

  }

//  "The uprn to address lookup service" should {
//
//    // This is used because you can't create real request object with invalid parameters
//    case class TestRequest(uprn: Long)
//
//    implicit val testRequestFormat = jsonFormat1(TestRequest)
//
//    "return a successful response containing a model for a valid uprn to address lookup request" in {
//      val request = TestRequest(uprnValid)
//      Post(uprntoAddressLookupUrl, request) ~> sealRoute(route) ~> check {
//        status should equal(OK)
//        val resp = responseAs[UprnToAddressResponse]
//        resp.addressViewModel should be (defined)
//        testValidAddressViewModel(resp.addressViewModel.get)
//      }
//    }
//
//  }

}