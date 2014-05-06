package dvla.microservice

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import java.net.URI
import org.scalatest.time.Span
import org.scalatest.time.Second
import dvla.domain.ordnance_survey.{OSAddressbaseHeader, OSAddressbaseDPA, OSAddressbaseResult, OSAddressbaseSearchResponse}
import dvla.domain.address_lookup.{PostcodeToAddressResponse, UprnToAddressLookupRequest, UprnAddressPair, PostcodeToAddressLookupRequest}
import dvla.helpers.UnitSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import scala.Some
import com.typesafe.config.{ConfigFactory, Config}
import akka.actor.ActorSystem
import spray.util.Utils

class OSAddressLookupCommandSpec extends UnitSpec {

  def testConfigSource: String = ""

  def testConfig: Config = {
    val source = testConfigSource
    val config = if (source.isEmpty) ConfigFactory.empty() else ConfigFactory.parseString(source)
    config.withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem(Utils.actorSystemNameFrom(getClass), testConfig)

  def actorRefFactory = system

  val header = OSAddressbaseHeader(uri = new URI(""),
    query = "",
    offset = 0,
    totalresults = 2,
    format = "",
    dataset = "",
    maxresults = 2)

  val postcodeValid = "CM81QJ"
  val traderUprnValid = 12345L

  def osAddressbaseDPA(uprn: String = traderUprnValid.toString, houseName: String = "presentationProperty stub", houseNumber: String = "123") = OSAddressbaseDPA(
    UPRN = uprn,
    address = s"$houseName, $houseNumber, property stub, street stub, town stub, area stub, $postcodeValid",
    buildingName = Some(houseName),
    buildingNumber = Some(houseNumber),
    postTown = "b",
    postCode = "c",
    RPC = "d",
    xCoordinate = 1f,
    yCoordinate = 2f,
    status = "e",
    matchScore = 3f,
    matchDescription = "f"
  )

  val configuration = Configuration("", "", "", 0)

  val oSAddressbaseResultsValidDPA = {
    val result = OSAddressbaseResult(DPA = Some(osAddressbaseDPA()), LPI = None)
    Seq(result, result, result)
  }

  val oSAddressbaseResultsEmptyDPAAndLPI = {
    val result = OSAddressbaseResult(DPA = None, LPI = None)
    Seq(result, result, result)
  }

  def osAddressLookupCommandMock(results: Option[Seq[OSAddressbaseResult]]): OSAddressLookupCommand = {
    new OSAddressLookupCommand(configuration) {
      override def callPostcodeToAddressOSWebService(request: PostcodeToAddressLookupRequest): Future[Option[OSAddressbaseSearchResponse]] = {
        Future.successful(Some(OSAddressbaseSearchResponse(header, results)))
      }

      override def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[OSAddressbaseSearchResponse]] = {
        Future.successful(Some(OSAddressbaseSearchResponse(header, results)))
      }
    }
  }


  "callPostcodeToAddressOSWebService" should {

    "return a valid sequence of UprnAddressPairs when the postcode is valid and the OS service returns results" in {

      val service = osAddressLookupCommandMock(Some(oSAddressbaseResultsValidDPA))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result, Timeout(Span(1, Second))) {
        r =>
          r.addresses.length should equal(oSAddressbaseResultsValidDPA.length)
          r.addresses should equal(oSAddressbaseResultsValidDPA.map(i => UprnAddressPair(i.DPA.get.UPRN, i.DPA.get.address)))
      }

    }

    "return an empty sequence when the postcode is valid but the OS service returns no results" in {

      val service = osAddressLookupCommandMock(None)
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r =>
          r.addresses shouldBe empty
      }

    }

    //    "return empty seq when response status is not 200 OK" in {
    //      val service = addressServiceMock(response(NOT_FOUND), Some(oSAddressbaseResultsValidDPA))
    //
    //      val result = service.fetchAddressesForPostcode(postcodeValid)
    //
    //      whenReady(result) {
    //        _ shouldBe empty
    //      }
    //    }
    //

    "return an empty sequence when the postcode is valid but the OS service returns a result with no DPA and no LPI" in {

      val service = osAddressLookupCommandMock(Some(oSAddressbaseResultsEmptyDPAAndLPI))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r =>
          r.addresses shouldBe empty
      }

    }

    //    "return empty seq when response throws" in {
    //      val addressLookupService = addressServiceMock(responseThrows, None)
    //
    //      val result = addressLookupService.fetchAddressesForPostcode(postcodeValid)
    //
    //      whenReady(result) {
    //        _ shouldBe empty
    //      }
    //    }
    //
    //    "return empty seq given invalid json" in {
    //      val inputAsJson = Json.toJson("INVALID")
    //      val service = addressServiceMock(response(OK, inputAsJson), Some(oSAddressbaseResultsValidDPA))
    //
    //      val result = service.fetchAddressesForPostcode(postcodeValid)
    //
    //      whenReady(result) {
    //        _ shouldBe empty
    //      }
    //    }
    //

    "not throw when an address contains a building number that contains letters" in {

      val expected = PostcodeToAddressResponse(Seq(
        UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123A, property stub, street stub, town stub, area stub, $postcodeValid"),
        UprnAddressPair(traderUprnValid.toString, s"presentationProperty BBB, 123B, property stub, street stub, town stub, area stub, $postcodeValid"),
        UprnAddressPair(traderUprnValid.toString, s"presentationProperty stub, 789C, property stub, street stub, town stub, area stub, $postcodeValid"))
      )
      val dpa1 = OSAddressbaseResult(DPA = Some(osAddressbaseDPA(houseNumber = "789C")), LPI = None)
      val dpa2 = OSAddressbaseResult(DPA = Some(osAddressbaseDPA(houseName = "presentationProperty BBB", houseNumber = "123B")), LPI = None)
      val dpa3 = OSAddressbaseResult(DPA = Some(osAddressbaseDPA(houseName = "presentationProperty AAA", houseNumber = "123A")), LPI = None)
      val oSAddressbaseResultsValidDPA = Seq(dpa1, dpa2, dpa3)

      val service = osAddressLookupCommandMock(Some(oSAddressbaseResultsValidDPA))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r =>
          r.addresses.length should equal(oSAddressbaseResultsValidDPA.length)
          r shouldBe expected
      }

    }

    "return seq of (uprn, address) sorted by building number then building name" in {

      val expected = PostcodeToAddressResponse(Seq(
        UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123, property stub, street stub, town stub, area stub, $postcodeValid"),
        UprnAddressPair(traderUprnValid.toString, s"presentationProperty BBB, 123, property stub, street stub, town stub, area stub, $postcodeValid"),
        UprnAddressPair(traderUprnValid.toString, s"presentationProperty stub, 789, property stub, street stub, town stub, area stub, $postcodeValid"))
      )
      val dpa1 = OSAddressbaseResult(DPA = Some(osAddressbaseDPA(houseNumber = "789")), LPI = None)
      val dpa2 = OSAddressbaseResult(DPA = Some(osAddressbaseDPA(houseName = "presentationProperty BBB", houseNumber = "123")), LPI = None)
      val dpa3 = OSAddressbaseResult(DPA = Some(osAddressbaseDPA(houseName = "presentationProperty AAA", houseNumber = "123")), LPI = None)
      val oSAddressbaseResultsValidDPA = Seq(dpa1, dpa2, dpa3)

      val service = osAddressLookupCommandMock(Some(oSAddressbaseResultsValidDPA))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r =>
          r.addresses.length should equal(oSAddressbaseResultsValidDPA.length)
          r shouldBe expected
      }
    }

  }

  "callUprnToAddressOSWebService" should {

    "return a valid AddressViewModel when the uprn is valid and the OS service returns results" in {

      val service = osAddressLookupCommandMock(Some(oSAddressbaseResultsValidDPA))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) {
        r => r.addressViewModel match {
          case Some(addressViewModel) =>
            addressViewModel.uprn.map(_.toString) should equal(Some(osAddressbaseDPA().UPRN))
            addressViewModel.address === osAddressbaseDPA().address
          case _ => fail("Should have returned Some(AddressViewModel)")
        }
      }
    }
  }

  //    "return None when response status is not 200 OK" in {
  //      val service = addressServiceMock(response(NOT_FOUND), Some(oSAddressbaseResultsValidDPA))
  //
  //      val result = service.fetchAddressForUprn(osAddressbaseDPA().UPRN)
  //
  //      whenReady(result) {
  //        _ should equal(None)
  //      }
  //    }

  "return none when the uprn is valid but the OS service returns no results" in {

    val service = osAddressLookupCommandMock(None)
    val result = service(UprnToAddressLookupRequest(traderUprnValid))

    whenReady(result) {
      r => r.addressViewModel should be(None)
    }
  }

  "return none when the result has no DPA and no LPI" in {

    val service = osAddressLookupCommandMock(Some(oSAddressbaseResultsEmptyDPAAndLPI))
    val result = service(UprnToAddressLookupRequest(traderUprnValid))

    whenReady(result) {
      r => r.addressViewModel should be(None)
    }
  }

  //    "return none when web service throws an exception" in {
  //      val addressLookupService = addressServiceMock(responseThrows, None)
  //
  //      val result = addressLookupService.fetchAddressForUprn(osAddressbaseDPA().UPRN)
  //
  //      whenReady(result) {
  //        _ should equal(None)
  //      }
  //    }
  //  }

}
