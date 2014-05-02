package dvla.microservice

import dvla.domain.JsonFormats._
import spray.http.StatusCodes._
import dvla.domain.address_lookup.{UprnAddressPair, AddressViewModel, UprnToAddressResponse, PostcodeToAddressResponse}
import org.mockito.Mockito._
import org.mockito.BDDMockito.given

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

    // This is used because you can't create real request object with invalid parameters
    case class TestRequest(postcode: String)

    implicit val testRequestFormat = jsonFormat1(TestRequest)
    val response = mock[PostcodeToAddressResponse]
    val request = TestRequest(postcodeValid)

    "return a successful response containing a model for a valid postcode to address lookup request" in {
      given(response.addresses) willReturn Seq(UprnAddressPair("12345","44 Hythe Road, White City, London, NW10 6RJ"))

      Post(postocdeToAddressLookupUrl, request) ~> sealRoute(route) ~> check {
      status should equal(OK)
      response.addresses(0).uprn should equal("12345")
      }
    }
  }

  "The uprn to address lookup service" should {

    // This is used because you can't create real request object with invalid parameters
    case class TestRequest(uprn: Long)

    implicit val testRequestFormat = jsonFormat1(TestRequest)
    val response = mock[UprnToAddressResponse]
    val request = TestRequest(uprnValid)

    "return a successful response containing a model for a valid uprn to address lookup request" in {
      given(response.addressViewModel) willReturn Some(AddressViewModel(Some(12345),List("44 Hythe Road, White City, London, NW10 6RJ")))
      Post(uprntoAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        response.addressViewModel should be (defined)
        testValidAddressViewModel(response.addressViewModel.get)
      }
    }
  }
}