package dvla.microservice

import dvla.domain.JsonFormats._
import spray.http.StatusCodes._
import dvla.domain.address_lookup._
import org.mockito.Mockito._
import org.mockito.BDDMockito.given
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.UprnToAddressResponse
import scala.Some

class OSAddressLookupServiceSpec extends RouteSpecBase {

  val postcodeValid = "SA11AA"
  val uprnValid = 12345L
  val postocdeToAddressLookupUrl = "/postcode-to-address"
  val uprntoAddressLookupUrl = "/uprn-to-address"


  private def testValidAddressViewModel(addressViewModel: AddressViewModel) = {
    addressViewModel.uprn.get should equal(12345)
    //TODO test address parts of the the model
  }


  "The postcode to address lookup service" should {

    val request = PostcodeToAddressLookupRequest(postcodeValid)

    "return a successful response containing a model for a valid postcode to address lookup request" in {
      Post(postocdeToAddressLookupUrl, request) ~> sealRoute(route) ~> check {
      status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses(0).uprn should equal("12345")
      }
    }
  }

  "The uprn to address lookup service" should {

    val request = UprnToAddressLookupRequest(uprnValid)

    "return a successful response containing a model for a valid uprn to address lookup request" in {
      Post(uprntoAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be (defined)
        testValidAddressViewModel(resp.addressViewModel.get)
      }
    }
  }

}