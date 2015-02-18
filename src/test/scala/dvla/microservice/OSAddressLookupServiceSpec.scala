package dvla.microservice

import dvla.domain.JsonFormats._
import dvla.domain.address_lookup._
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnToAddressResponse
import org.mockito.Mockito._
import scala.concurrent.Future
import spray.http.StatusCodes._

class OSAddressLookupServiceSpec extends RouteSpecBase {
  "The postcode to address lookup service" should {
    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid postcode to address lookup request" in {
      val postcodeToAddressResponse = PostcodeToAddressResponse(fetchedAddressesSeq)
      when(command.apply(postcodeValidRequest)).thenReturn(Future.successful(postcodeToAddressResponse))

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(fetchedAddressesSeq)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {
      when(command.apply(postcodeValidRequest)).thenReturn(Future.failed(new RuntimeException))

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid") ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return empty address Seq when that postcode does not exist" in {
      val postcodeToAddressResponse = PostcodeToAddressResponse(addresses = Seq.empty)
      when(command.apply(postcodeValidRequest)).thenReturn(Future.successful(postcodeToAddressResponse))

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(Seq.empty)
      }
    }

    "return addresses without language filter when no addresses existed for the filtered lookup" in {
      val noAddressesFound = PostcodeToAddressResponse(addresses = Seq.empty)
      val addressesFound = PostcodeToAddressResponse(addresses = fetchedAddressesSeq)
      when(command.apply(postcodeValidRequest)).thenReturn(
        Future.successful(addressesFound)
      )
      when(command.apply(postcodeValidLanguageCyRequest)).thenReturn(
        Future.successful(noAddressesFound)
      )

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid&languageCode=cy") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(fetchedAddressesSeq)
      }
    }
  }

  "The uprn to address lookup service" should {
    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid uprn to address lookup request" in {
      val uprnToAddressResponse = UprnToAddressResponse(Option(fetchedAddressViewModel))
      when(command.apply(uprnValidRequest)).thenReturn(Future.successful(uprnToAddressResponse))

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be(defined)
        resp.addressViewModel.get should equal(fetchedAddressViewModel)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {
      when(command.apply(uprnValidRequest)).thenReturn(Future.failed(new RuntimeException))

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid") ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return None when that postcode does not exist" in {
      val uprnToAddressResponse = UprnToAddressResponse(None)
      when(command.apply(uprnValidRequest)).thenReturn(Future.successful(uprnToAddressResponse))

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should equal(None)
      }
    }

    "return address without language filter when no addresses existed for the filtered lookup" in {
      val noAddressFound = UprnToAddressResponse(addressViewModel = None)
      val addressFound = UprnToAddressResponse(Option(fetchedAddressViewModel))
      when(command.apply(uprnValidRequest)).thenReturn(
        Future.successful(addressFound)
      )
      when(command.apply(uprnValidLanguageCyRequest)).thenReturn(
        Future.successful(noAddressFound)
      )

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid&languageCode=cy") ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be(defined)
        resp.addressViewModel.get should equal(fetchedAddressViewModel)
      }
    }


  }

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
  private val postcodeValidRequest = PostcodeToAddressLookupRequest(postcode = postcodeValid, showBusinessName = Some(false))
  private val postcodeValidLanguageCyRequest = PostcodeToAddressLookupRequest(postcodeValid, languageCode = Some("cy"), showBusinessName = Some(false))
  private val uprnValidRequest = UprnToAddressLookupRequest(uprnValid)
  private val uprnValidLanguageCyRequest = UprnToAddressLookupRequest(uprnValid, languageCode = Some("cy"))
}