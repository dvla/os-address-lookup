package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, Config}
import dvla.domain.address_lookup._
import dvla.domain.ordnance_survey_preproduction.{Header, DPA, Result, Response}
import dvla.helpers.UnitSpec
import dvla.microservice.Configuration
import java.net.URI
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.Second
import org.scalatest.time.Span
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class LookupCommandSpec extends UnitSpec {

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
      val dpa = {
        val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingNumber = Some("50ABC"), thoroughfareName = Some("FAKE ROAD"), postTown = "FAKE TOWN", postCode = "EX8 1SN")), LPI = None)
        Seq(dpa1)
      }

      val service = lookupCommandMock(Some(Response(header, Some(dpa))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) {
        r => r.addresses.length should equal(dpa.length)
          r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"50ABC FAKE ROAD, FAKE TOWN, EX8 1SN")))
      }
    }
  }

  "FLAT 1, 52, SALISBURY ROAD, EXMOUTH, EX8 1SN should return in the format FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("FLAT 1"), buildingNumber = Some("52"), thoroughfareName = Some("SALISBURY ROAD"), postTown = "EXMOUTH", postCode = "EX8 1SN")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN")))
    }
  }

  "FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP should return in the format FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("FLAT 1"), buildingName = Some("MONTPELLIER COURT"), thoroughfareName = Some("MONTPELLIER ROAD"), postTown = "EXMOUTH", postCode = "EX8 1JP")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP")))
    }
  }

  "FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB should return in the format FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("FLAT 1"), buildingName = Some("13A"), thoroughfareName = Some("CRANLEY GARDENS"), postTown = "LONDON", postCode = "SW7 3BB")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB")))
    }
  }

  "UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS should return in the format UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingName = Some("UNIT 1-2"), dependentThoroughfareName = Some("DINAN WAY TRADING ESTATE"), thoroughfareName = Some("CONCORDE ROAD"), postTown = "EXMOUTH", postCode = "EX8 4RS")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS")))
    }
  }

  "6, BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ should return in the format 6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingNumber = Some("6"), dependentThoroughfareName = Some("BRIXINGTON PARADE"), thoroughfareName = Some("CHURCHILL ROAD"), postTown = "EXMOUTH", postCode = "EX8 4RJ")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ")))
    }
  }

  "6, PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY should return in the format 6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingNumber = Some("6"), dependentThoroughfareName = Some("PARK VIEW"), thoroughfareName = Some("WOTTON LANE"), dependentLocality = Some("LYMPSTONE"), postTown = "EXMOUTH", postCode = "EX8 5LY")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY")))
    }
  }

  "7, VILLA MAISON, 4, CYPRUS ROAD, EXMOUTH, EX8 2DZ should return in the format 7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("7"), buildingName = Some("VILLA MAISON"), buildingNumber = Some("4"), thoroughfareName = Some("CYPRUS ROAD"), postTown = "EXMOUTH", postCode = "EX8 2DZ")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ")))
    }
  }

  "FLAT 1,HEATHGATE,7,LANSDOWNE ROAD,BUDLEIGH SALTERTON,EX9 6AH should return in the format FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("FLAT 1"), buildingName = Some("HEATHGATE"), buildingNumber = Some("7"), thoroughfareName = Some("LANSDOWNE ROAD"), postTown = "BUDLEIGH SALTERTON", postCode = "EX9 6AH")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH")))
    }
  }



  "FLAT, COURTLANDS CROSS SERVICE STATION, 397, EXETER ROAD, EXMOUTH, EX8 3NS should return in the format FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("FLAT"), buildingName = Some("COURTLANDS CROSS SERVICE STATION"), buildingNumber = Some("397"), thoroughfareName = Some("EXETER ROAD"), postTown = "EXMOUTH", postCode = "EX8 3NS")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS")))
    }
  }

  "2, THE RED LODGE, 11, ELWYN ROAD, EXMOUTH, EX8 2EL should return in the format 2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(subBuildingName = Some("2"), buildingName = Some("THE RED LODGE"), buildingNumber = Some("11"), thoroughfareName = Some("ELWYN ROAD"), postTown = "EXMOUTH", postCode = "EX8 2EL")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL")))
    }
  }

  "40, SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN should return in the format 40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingNumber = Some("40"), thoroughfareName = Some("SKETTY PARK DRIVE"), dependentLocality = Some("SKETTY"), postTown = "SWANSEA", postCode = "SA2 8LN")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN")))
    }
  }

  "4, LYNDHURST ROAD, EXMOUTH, EX8 3DT should return in the format 4 LYNDHURST ROAD, EXMOUTH, EX8 3DT" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingNumber = Some("4"), thoroughfareName = Some("LYNDHURST ROAD"), postTown = "EXMOUTH", postCode = "EX8 3DT")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"4 LYNDHURST ROAD, EXMOUTH, EX8 3DT")))
    }
  }

  "ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ should return in the format ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ" in {
    val dpa = {
      val dpa1 = Result(DPA = Some(osAddressbaseDPA(buildingName = Some("ASH COTTAGE"), thoroughfareName = Some("OLD BYSTOCK DRIVE"), dependentLocality = Some("BYSTOCK"), postTown = "EXMOUTH", postCode = "EX8 5EQ")), LPI = None)
      Seq(dpa1)
    }

    val service = lookupCommandMock(Some(Response(header, Some(dpa))))
    val result = service(PostcodeToAddressLookupRequest(postcodeValid))

    whenReady(result) {
      r => r.addresses.length should equal(dpa.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ")))
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

  private def testConfig: Config = {
    ConfigFactory.empty().withFallback(ConfigFactory.load())
  }

  private implicit val system = ActorSystem("LookupCommandSpecPreProduction", testConfig)

  private val header = Header(
    uri = new URI(""),
    offset = 0,
    totalresults = 2)

  private final val traderUprnValid = 12345L
  private final val postcodeValid = "CM81QJ"
  private final val emptyString = ""

  private def osAddressbaseDPA(uprn: String = traderUprnValid.toString, address: String = emptyString, poBoxNumber: Option[String] = None,
                               buildingName: Option[String] = None, subBuildingName: Option[String] = None, buildingNumber: Option[String] = None,
                               thoroughfareName: Option[String] = None, dependentThoroughfareName: Option[String] = None, dependentLocality: Option[String] = None,
                               postTown: String = emptyString, postCode: String = emptyString) =

    DPA(UPRN = uprn, address = address, poBoxNumber = poBoxNumber, buildingName = buildingName, subBuildingName = subBuildingName, buildingNumber = buildingNumber,
      thoroughfareName = thoroughfareName, dependentThoroughfareName = dependentThoroughfareName, dependentLocality = dependentLocality, postTown = postTown,
      postCode = postCode, RPC = emptyString, xCoordinate = 0, yCoordinate = 0, status = emptyString, matchScore = 0, matchDescription = emptyString)

  private val configuration = Configuration()
  private val postcodeUrlBuilder = new PostcodeUrlBuilder(configuration = configuration)
  private val uprnUrlBuilder = new UprnUrlBuilder(configuration = configuration)

  private val validDPANoLPI = {
    val result = Result(DPA = Some(osAddressbaseDPA()), LPI = None)
    Seq(result, result, result)
  }

  private val threeAddressPairs = {
    val result = UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123A, property stub, street stub, town stub, area stub, $postcodeValid")
    Seq(result, result, result)
  }

  private val emptyDPAandLPI = {
    val result = Result(DPA = None, LPI = None)
    Seq(result, result, result)
  }

  private def lookupCommandMock(response: Option[Response]): LookupCommand = {
    new LookupCommand(configuration = configuration, postcodeUrlBuilder = postcodeUrlBuilder, uprnUrlBuilder = uprnUrlBuilder) {
      override def callPostcodeToAddressOSWebService(request: PostcodeToAddressLookupRequest): Future[Option[Response]] = Future.successful(response)

      override def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[Response]] = Future.successful(response)
    }
  }

  private def lookupCommandMock(
                                 postcodeResponse: PostcodeToAddressResponse = PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"presentationProperty AAA, 123A, property stub, street stub, town stub, area stub, $postcodeValid"))),
                                 uprnResponse: UprnToAddressResponse = UprnToAddressResponse(addressViewModel = None)): LookupCommand = {

    new LookupCommand(configuration, postcodeUrlBuilder = postcodeUrlBuilder, uprnUrlBuilder = uprnUrlBuilder) {
      override def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = Future.successful(postcodeResponse)

      override def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = Future.successful(uprnResponse)
    }
  }
}

