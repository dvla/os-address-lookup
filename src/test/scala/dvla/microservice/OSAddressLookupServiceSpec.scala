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
import dvla.domain.ordnance_survey_beta_0_6.Response

final class OSAddressLookupServiceSpec extends RouteSpecBase {

  // test data
  private final val postcodeValid = "SA11AA"
  private final val uprnValid = 12345L
  private final val postcodeToAddressLookupUrl = "/postcode-to-address"
  private final val uprnToAddressLookupUrl = "/uprn-to-address"
  private final val traderUprnValid = 12345L
  private final val traderUprnValid2 = 4567L
  private val addressWithUprn = AddressViewModel(uprn = Some(traderUprnValid), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))
  private val fetchedAddressesSeq = Seq(
    UprnAddressPair(traderUprnValid.toString, addressWithUprn.address.mkString(", ")),
    UprnAddressPair(traderUprnValid2.toString, addressWithUprn.address.mkString(", "))
  )
  private val fetchedAddressViewModel = AddressViewModel(uprn = Some(traderUprnValid), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))


  "The postcode to address lookup service" should {

    val request = PostcodeToAddressLookupRequest(postcodeValid)

    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid postcode to address lookup request" in {

      val postcodeToAddressResponse = PostcodeToAddressResponse(fetchedAddressesSeq)
      when(command.apply(request)).thenReturn(Future.successful(postcodeToAddressResponse))

      Get(s"${postcodeToAddressLookupUrl}?postcode=${postcodeValid}") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(fetchedAddressesSeq)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {

      when(command.apply(request)).thenReturn(Future.failed(new RuntimeException))

      Get(s"${postcodeToAddressLookupUrl}?postcode=${postcodeValid}") ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return empty address Seq when that postcode does not exist" in {
      val postcodeToAddressResponse = PostcodeToAddressResponse(addresses = Seq.empty)
      when(command.apply(request)).thenReturn(Future.successful(postcodeToAddressResponse))

      Get(s"${postcodeToAddressLookupUrl}?postcode=${postcodeValid}") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(Seq.empty)
      }
    }
  }

  "The uprn to address lookup service" should {

    val request = UprnToAddressLookupRequest(uprnValid)

    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid uprn to address lookup request" in {

      val uprnToAddressResponse = UprnToAddressResponse(Option(fetchedAddressViewModel))
      when(command.apply(request)).thenReturn(Future.successful(uprnToAddressResponse))

      Get(s"${uprnToAddressLookupUrl}?uprn=${uprnValid}") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be(defined)
        resp.addressViewModel.get should equal(fetchedAddressViewModel)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {

      when(command.apply(request)).thenReturn(Future.failed(new RuntimeException))

      Get(s"${uprnToAddressLookupUrl}?uprn=${uprnValid}") ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

  }


}