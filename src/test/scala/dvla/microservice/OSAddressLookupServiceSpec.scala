package dvla.microservice

import dvla.common.clientsidesession.TrackingId
import dvla.common.microservice.HttpHeaders.`Tracking-Id`
import dvla.domain.JsonFormats._
import dvla.domain.address_lookup._
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnToAddressResponse
import org.mockito.Mockito._
import scala.concurrent.Future
import spray.http.StatusCodes._

class OSAddressLookupServiceSpec extends RouteSpecBase {
  "Lookup address by postcode and return json" should {
    val fetchedAddressesSeq = Seq(
      AddressDto(
        addressLine = "Business Name 1, address line 1, address line 2, address line 3, London, QQ99QQ",
        businessName = Some("Business Name 1"),
        streetAddress1 = "address line 1",
        streetAddress2 = Some("address line 2"),
        streetAddress3 = Some("address line 3"),
        postTown = "London",
        postCode = "QQ99QQ"
      ),
      AddressDto(
        addressLine = "Business Name 2, address line 1, address line 2, address line 3, Swansea, QQ10QQ",
        businessName = Some("Business Name 2"),
        streetAddress1 = "address line 1",
        streetAddress2 = Some("address line 2"),
        streetAddress3 = Some("address line 3"),
        postTown = "Swansea",
        postCode = "QQ10QQ"
      )
    )

    val request = PostcodeToAddressLookupRequest(postcode = postcodeValid, None, showBusinessName = Some(true))


    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid postcode to address lookup request" in {
      val response = fetchedAddressesSeq
      when(command.applyDetailedResult(request)).thenReturn(Future.successful(response))

      Get(s"$addressesLookupUrl?postcode=$postcodeValid")  ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        contentType.toString() should equal("application/json; charset=UTF-8")
        val resp = responseAs[Seq[AddressDto]]
        resp should equal(fetchedAddressesSeq)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {
      when(command.applyDetailedResult(request)).thenReturn(Future.failed(new RuntimeException))

      Get(s"$addressesLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return empty address Seq when that postcode does not exist" in {
      when(command.applyDetailedResult(request)).thenReturn(Future.successful(Seq.empty))

      Get(s"$addressesLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        contentType.toString() should equal("application/json; charset=UTF-8")
        val resp = responseAs[Seq[AddressDto]]
        resp should equal(Seq.empty)
      }
    }

    "return addresses without language filter when no addresses existed for the filtered lookup" in {
      val noAddressesFound = PostcodeToAddressResponse(addresses = Seq.empty)
      val cyRequest = PostcodeToAddressLookupRequest(postcodeValid, Some("cy"), showBusinessName = Some(true))
      when(command.applyDetailedResult(request)).thenReturn(
        Future.successful(fetchedAddressesSeq)
      )
      when(command.applyDetailedResult(cyRequest)).thenReturn(
        Future.successful(Seq.empty)
      )

      Get(s"$addressesLookupUrl?postcode=$postcodeValid&languageCode=cy") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        contentType.toString() should equal("application/json; charset=UTF-8")
        responseAs[Seq[AddressDto]] should equal(fetchedAddressesSeq)
      }
    }
  }

  "The postcode to address lookup service" should {
    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid postcode to address lookup request" in {
      val postcodeToAddressResponse = PostcodeToAddressResponse(fetchedAddressesSeq)
      when(command.apply(postcodeValidRequest)).thenReturn(Future.successful(postcodeToAddressResponse))

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[PostcodeToAddressResponse]
        resp.addresses should equal(fetchedAddressesSeq)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {
      when(command.apply(postcodeValidRequest)).thenReturn(Future.failed(new RuntimeException))

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return empty address Seq when that postcode does not exist" in {
      val postcodeToAddressResponse = PostcodeToAddressResponse(addresses = Seq.empty)
      when(command.apply(postcodeValidRequest)).thenReturn(Future.successful(postcodeToAddressResponse))

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
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

      Get(s"$postcodeToAddressLookupUrl?postcode=$postcodeValid&languageCode=cy") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
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

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        val resp = responseAs[UprnToAddressResponse]
        resp.addressViewModel should be(defined)
        resp.addressViewModel.get should equal(fetchedAddressViewModel)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {
      when(command.apply(uprnValidRequest)).thenReturn(Future.failed(new RuntimeException))

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return None when that postcode does not exist" in {
      val uprnToAddressResponse = UprnToAddressResponse(None)
      when(command.apply(uprnValidRequest)).thenReturn(Future.successful(uprnToAddressResponse))

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
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

      Get(s"$uprnToAddressLookupUrl?uprn=$uprnValid&languageCode=cy") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
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
  private final val addressesLookupUrl = "/addresses"
  private final val traderUprnValid = 12345L
  private final val traderUprnValid2 = 4567L
  implicit val trackingId = TrackingId("default_tracking_id")
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