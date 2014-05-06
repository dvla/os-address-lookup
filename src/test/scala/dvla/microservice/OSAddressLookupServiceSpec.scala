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
import scala.concurrent.Future
import dvla.domain.ordnance_survey.OSAddressbaseSearchResponse

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

      val traderUprnValid1 = 12345L
      val traderUprnValid2 = 4567L
      val addressWithUprn = AddressViewModel(uprn = Some(traderUprnValid1), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))
      val fetchedAddresses = Seq(
        UprnAddressPair(traderUprnValid1.toString, addressWithUprn.address.mkString(", ")),
        UprnAddressPair(traderUprnValid2.toString, addressWithUprn.address.mkString(", "))
      )
      val postcodeToAddressResponse = PostcodeToAddressResponse(fetchedAddresses)
      when(command.apply(request)).thenReturn(Future.successful(postcodeToAddressResponse))

      Post(postocdeToAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses(0).uprn should equal("12345")
      }
    }

    "return an unsuccessful response containing a Service Unavailable status code when the command throws an exception" in {

    when(command.apply(request)).thenReturn(Future.failed(new RuntimeException))

      Post(postocdeToAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

  }

  "The uprn to address lookup service" should {

    val request = UprnToAddressLookupRequest(uprnValid)

    "return a successful response containing a model for a valid uprn to address lookup request" in {

      val traderUprnValid = 12345L
      val fetchedAddress = AddressViewModel(uprn = Some(traderUprnValid), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))
      val uprnToAddressResponse = UprnToAddressResponse(Option(fetchedAddress))
      when(command.apply(request)).thenReturn(Future.successful(uprnToAddressResponse))

      Post(uprntoAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be(defined)
        testValidAddressViewModel(resp.addressViewModel.get)
      }
    }

    "return an unsuccessful response containing a Service Unavailable status code when the command throws an exception" in {

      when(command.apply(request)).thenReturn(Future.failed(new RuntimeException))

      Post(uprntoAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

  }


}