package dvla.microservice.ordnance_survey_preproduction

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import java.net.URI
import org.scalatest.time.Span
import org.scalatest.time.Second
import dvla.domain.ordnance_survey_preproduction.{Header, DPA, Result, Response}
import dvla.domain.address_lookup._
import dvla.helpers.UnitSpec
import com.typesafe.config.{ConfigFactory, Config}
import akka.actor.ActorSystem
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import dvla.domain.address_lookup.UprnToAddressLookupRequest
import scala.Some
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.microservice.Configuration
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest

class LookupCommandSpec extends UnitSpec {

  "callPostcodeToAddressOSWebService" should {

    "return ordnance_survey valid sequence of UprnAddressPairs when the postcode is valid and the OS service returns results" in {
      val service = lookupCommandMock(postcodeResponse = PostcodeToAddressResponse(threeAddressPairs))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result, Timeout(Span(1, Second))) {
        r => r.addresses.length should equal(validDPANoLPI.length)
             r.addresses.foreach(a => a.uprn should equal(traderUprnValid.toString))
      }
    }

    "return an empty sequence when the postcode is valid but the OS service returns no results" in {
      val service = lookupCommandMock(postcodeResponse = PostcodeToAddressResponse(Seq.empty))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r => r.addresses shouldBe empty
      }
    }

    "return empty seq when response status is not 200 OK" in {
      val service = lookupCommandMock(None)
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r => r.addresses shouldBe empty
      }
    }

    "return an empty sequence when the postcode is valid but the OS service returns ordnance_survey result with no DPA and no LPI" in {
      val service = lookupCommandMock(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r => r.addresses shouldBe empty
      }
    }

    "not throw when an address contains ordnance_survey building number that contains letters" in {
      val service = lookupCommandMock(Some(Response(header, Some(oSAddressbaseResultsValidDPA("a")))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r => r.addresses.length should equal(oSAddressbaseResultsValidDPA("a").length)
             r shouldBe expected("a")
      }
    }

    "return seq of (uprn, address) sorted by building number then building name" in {
      val service = lookupCommandMock(Some(Response(header, Some(oSAddressbaseResultsValidDPA()))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r => r.addresses.length should equal(oSAddressbaseResultsValidDPA().length)
             r shouldBe expected()
      }
    }
  }

  "callUprnToAddressOSWebService" should {

    "return ordnance_survey valid AddressViewModel when the uprn is valid and the OS service returns results" in {
      val service = lookupCommandMock(Some(Response(header, Some(validDPANoLPI))))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) {
        r => r.addressViewModel match {
          case Some(addressViewModel) => addressViewModel.uprn.map(_.toString) should equal(Some(osAddressbaseDPA().UPRN))
                                         addressViewModel.address === osAddressbaseDPA().address
          case _ => fail("Should have returned Some(AddressViewModel)")
        }
      }
    }

    "return None when response status is not 200 OK" in {
      val service = lookupCommandMock(None)
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) {
        r => r.addressViewModel should be(None)
      }
    }

    "return none when the uprn is valid but the OS service returns no results" in {
      val service = lookupCommandMock(Some(Response(header, None)))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) {
        r => r.addressViewModel should be(None)
      }
    }

    "return none when the result has no DPA and no LPI" in {
      val service = lookupCommandMock(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) {
        r => r.addressViewModel should be(None)
      }
    }
  }

  def testConfig: Config = {
    ConfigFactory.empty().withFallback(ConfigFactory.load())
  }

  implicit val system = ActorSystem("LookupCommandSpecPreProduction", testConfig)

  val header = Header(
    uri = new URI(""),
    query = "",
    offset = 0,
    totalresults = 2,
    format = "",
    dataset = "",
    maxresults = 2)

  val postcodeValid = "CM81QJ"
  val traderUprnValid = 12345L

  def osAddressbaseDPA(uprn: String = traderUprnValid.toString, houseName: String = "presentationProperty stub", houseNumber: String = "123") = DPA(
    UPRN = uprn,
    address = s"$houseName, $houseNumber, property stub, street stub, town stub, area stub, $postcodeValid",
    buildingNumber = Some(houseNumber)
  )

  val configuration = Configuration("", "", "")

  val validDPANoLPI = {
    val result = Result(DPA = Some(osAddressbaseDPA()), LPI = None)
    Seq(result, result, result)
  }

  val threeAddressPairs = {
    val result = UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123A, property stub, street stub, town stub, area stub, $postcodeValid")
    Seq(result, result, result)
  }

  val emptyDPAandLPI = {
    val result = Result(DPA = None, LPI = None)
    Seq(result, result, result)
  }

  def lookupCommandMock(response: Option[Response]): LookupCommand = {
    new LookupCommand(configuration) {
      override def callPostcodeToAddressOSWebService(request: PostcodeToAddressLookupRequest): Future[Option[Response]] = Future.successful(response)

      override def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[Response]] = Future.successful(response)
    }
  }

  def lookupCommandMock(
                         postcodeResponse: PostcodeToAddressResponse = PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123A, property stub, street stub, town stub, area stub, $postcodeValid"))),
                         uprnResponse: UprnToAddressResponse = UprnToAddressResponse(addressViewModel = None)): LookupCommand = {

    new LookupCommand(configuration) {
      override def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = Future.successful(postcodeResponse)

      override def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = Future.successful(uprnResponse)
    }
  }

  private def expected(extraChars: String = "") = {
    PostcodeToAddressResponse(Seq(
      UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123$extraChars, property stub, street stub, town stub, area stub, $postcodeValid"),
      UprnAddressPair(traderUprnValid.toString, s"presentationProperty BBB, 123$extraChars, property stub, street stub, town stub, area stub, $postcodeValid"),
      UprnAddressPair(traderUprnValid.toString, s"presentationProperty stub, 789$extraChars, property stub, street stub, town stub, area stub, $postcodeValid")))
  }

  private def oSAddressbaseResultsValidDPA(extraChars: String = "") = {
    val dpa1 = Result(DPA = Some(osAddressbaseDPA(houseNumber = s"789$extraChars")), LPI = None)
    val dpa2 = Result(DPA = Some(osAddressbaseDPA(houseName = "presentationProperty BBB", houseNumber = s"123$extraChars")), LPI = None)
    val dpa3 = Result(DPA = Some(osAddressbaseDPA(houseName = "presentationProperty AAA", houseNumber = s"123$extraChars")), LPI = None)
    Seq(dpa1, dpa2, dpa3)
  }
}

