package dvla.microservice

import dvla.common.clientsidesession.TrackingId
import dvla.common.microservice.HttpHeaders.`Tracking-Id`
import dvla.domain.JsonFormats._
import dvla.domain.address_lookup.{AddressDto, PostcodeToAddressLookupRequest}
import org.mockito.Mockito.when
import spray.http.StatusCodes.{OK, ServiceUnavailable}

import scala.concurrent.Future

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

    val request = PostcodeToAddressLookupRequest(postcode = postcodeValid, None)

    "return ordnance_survey successful response containing ordnance_survey model for ordnance_survey valid postcode to address lookup request" in {
      val response = fetchedAddressesSeq
      when(command(request)).thenReturn(Future.successful(response))

      Get(s"$addressesLookupUrl?postcode=$postcodeValid")  ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        contentType.toString() should equal("application/json; charset=UTF-8")
        val resp = responseAs[Seq[AddressDto]]
        resp should equal(fetchedAddressesSeq)
      }
    }

    "return an unsuccessful response containing ordnance_survey Service Unavailable status code when the command throws an exception" in {
      when(command(request)).thenReturn(Future.failed(new RuntimeException))

      Get(s"$addressesLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(ServiceUnavailable)
      }
    }

    "return empty address Seq when that postcode does not exist" in {
      when(command(request)).thenReturn(Future.successful(Seq.empty))

      Get(s"$addressesLookupUrl?postcode=$postcodeValid") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        contentType.toString() should equal("application/json; charset=UTF-8")
        val resp = responseAs[Seq[AddressDto]]
        resp should equal(Seq.empty)
      }
    }

    "return addresses without language filter when no addresses existed for the filtered lookup" in {
      val noAddressesFound = Seq.empty
      val cyRequest = PostcodeToAddressLookupRequest(postcodeValid, Some("cy"))
      when(command(request)).thenReturn(
        Future.successful(fetchedAddressesSeq)
      )
      when(command(cyRequest)).thenReturn(
        Future.successful(Seq.empty)
      )

      Get(s"$addressesLookupUrl?postcode=$postcodeValid&languageCode=cy") ~> addHeader(`Tracking-Id`.name, trackingId.value) ~> sealRoute(route) ~> check {
        status should equal(OK)
        contentType.toString() should equal("application/json; charset=UTF-8")
        responseAs[Seq[AddressDto]] should equal(fetchedAddressesSeq)
      }
    }
  }

  private final val postcodeValid = "SA11AA"
  private final val addressesLookupUrl = "/addresses"
  implicit val trackingId = TrackingId("default_tracking_id")
}