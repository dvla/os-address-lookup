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

  // test data
  val postcodeValid = "SA11AA"
  val uprnValid = 12345L
  val postocdeToAddressLookupUrl = "/postcode-to-address"
  val uprntoAddressLookupUrl = "/uprn-to-address"
  val traderUprnValid = 12345L
  val traderUprnValid2 = 4567L
  val addressWithUprn = AddressViewModel(uprn = Some(traderUprnValid), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))
  val fetchedAddressesSeq = Seq(
    UprnAddressPair(traderUprnValid.toString, addressWithUprn.address.mkString(", ")),
    UprnAddressPair(traderUprnValid2.toString, addressWithUprn.address.mkString(", "))
  )
  val fetchedAddressViewModel = AddressViewModel(uprn = Some(traderUprnValid), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))


  "The postcode to address lookup service" should {

    val request = PostcodeToAddressLookupRequest(postcodeValid)

    "return a successful response containing a model for a valid postcode to address lookup request" in {

      val postcodeToAddressResponse = PostcodeToAddressResponse(fetchedAddressesSeq)
      when(command.apply(request)).thenReturn(Future.successful(postcodeToAddressResponse))

      Post(postocdeToAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(fetchedAddressesSeq)
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

      val uprnToAddressResponse = UprnToAddressResponse(Option(fetchedAddressViewModel))
      when(command.apply(request)).thenReturn(Future.successful(uprnToAddressResponse))

      Post(uprntoAddressLookupUrl, request) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be(defined)
        resp.addressViewModel.get should equal(fetchedAddressViewModel)
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