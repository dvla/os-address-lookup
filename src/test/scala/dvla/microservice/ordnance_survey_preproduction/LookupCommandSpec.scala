package dvla.microservice.ordnance_survey_preproduction

import java.net.URI
import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import dvla.domain.address_lookup._
import dvla.domain.ordnance_survey_preproduction.{DPA, Header, Response, Result}
import dvla.helpers.UnitSpec
import dvla.microservice.ordnance_survey_preproduction.LookupCommand._
import dvla.microservice.{AddressLookupCommand, Configuration}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Second, Span}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class LookupCommandSpec extends UnitSpec with MockitoSugar {

  "call PostcodeToAddressResponse" should {

    "return ordnance_survey valid sequence of UprnAddressPairs when the postcode is valid and the OS service returns seq of results" in {
      val osResult = resultBuilder(organisationName = Some("DVLA"), buildingName = Some("ASH COTTAGE"), thoroughfareName = Some("OLD BYSTOCK DRIVE"), dependentLocality = Some("BYSTOCK"), postTown = "EXMOUTH", postCode = "EX8 5EQ")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult ++ osResult ++ osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result, Timeout(Span(1, Second))) { r =>
        r.addresses.foreach(a => a.uprn should equal(traderUprnValid.toString))
      }
    }

    "return an empty sequence when the postcode is valid but the OS service returns no results" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(Seq.empty))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses shouldBe empty
      }
    }

    "return empty seq when response status is not 200 OK" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(None)
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses shouldBe empty
      }
    }

    "return an empty sequence when the postcode is valid but the OS service returns ordnance_survey result with no DPA and no LPI" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses shouldBe empty
      }
    }

    "not throw when an address contains ordnance_survey building number that contains letters" in {
      val osResult = resultBuilder(buildingNumber = Some("50ABC"), thoroughfareName = Some("FAKE ROAD"), postTown = "FAKE TOWN", postCode = "EX8 1SN")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"50ABC FAKE ROAD, FAKE TOWN, EX8 1SN")))
      }
    }

    "FLAT 1, 52, SALISBURY ROAD, EXMOUTH, EX8 1SN should return in the format FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN" in {
      val osResult = resultBuilder(subBuildingName = Some("FLAT 1"), buildingNumber = Some("52"), thoroughfareName = Some("SALISBURY ROAD"), postTown = "EXMOUTH", postCode = "EX8 1SN")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN")))
      }
    }

    "FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP should return in the format FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP" in {
      val osResult = resultBuilder(subBuildingName = Some("FLAT 1"), buildingName = Some("MONTPELLIER COURT"), thoroughfareName = Some("MONTPELLIER ROAD"), postTown = "EXMOUTH", postCode = "EX8 1JP")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP")))
      }
    }

    "FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB should return in the format FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB" in {
      val osResult = resultBuilder(subBuildingName = Some("FLAT 1"), buildingName = Some("13A"), thoroughfareName = Some("CRANLEY GARDENS"), postTown = "LONDON", postCode = "SW7 3BB")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB")))
      }
    }

    "UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS should return in the format UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS" in {
      val osResult = resultBuilder(buildingName = Some("UNIT 1-2"), dependentThoroughfareName = Some("DINAN WAY TRADING ESTATE"), thoroughfareName = Some("CONCORDE ROAD"), postTown = "EXMOUTH", postCode = "EX8 4RS")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS")))
      }
    }

    "6, BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ should return in the format 6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ" in {
      val osResult = resultBuilder(buildingNumber = Some("6"), dependentThoroughfareName = Some("BRIXINGTON PARADE"), thoroughfareName = Some("CHURCHILL ROAD"), postTown = "EXMOUTH", postCode = "EX8 4RJ")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ")))
      }
    }

    "6, PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY should return in the format 6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY" in {
      val osResult = resultBuilder(buildingNumber = Some("6"), dependentThoroughfareName = Some("PARK VIEW"), thoroughfareName = Some("WOTTON LANE"), dependentLocality = Some("LYMPSTONE"), postTown = "EXMOUTH", postCode = "EX8 5LY")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY")))
      }
    }

    "7, VILLA MAISON, 4, CYPRUS ROAD, EXMOUTH, EX8 2DZ should return in the format 7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ" in {
      val osResult = resultBuilder(subBuildingName = Some("7"), buildingName = Some("VILLA MAISON"), buildingNumber = Some("4"), thoroughfareName = Some("CYPRUS ROAD"), postTown = "EXMOUTH", postCode = "EX8 2DZ")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ")))
      }
    }

    "FLAT 1,HEATHGATE,7,LANSDOWNE ROAD,BUDLEIGH SALTERTON,EX9 6AH should return in the format FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH" in {
      val osResult = resultBuilder(subBuildingName = Some("FLAT 1"), buildingName = Some("HEATHGATE"), buildingNumber = Some("7"), thoroughfareName = Some("LANSDOWNE ROAD"), postTown = "BUDLEIGH SALTERTON", postCode = "EX9 6AH")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH")))
      }
    }

    "FLAT, COURTLANDS CROSS SERVICE STATION, 397, EXETER ROAD, EXMOUTH, EX8 3NS should return in the format FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS" in {
      val osResult = resultBuilder(subBuildingName = Some("FLAT"), buildingName = Some("COURTLANDS CROSS SERVICE STATION"), buildingNumber = Some("397"), thoroughfareName = Some("EXETER ROAD"), postTown = "EXMOUTH", postCode = "EX8 3NS")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS")))
      }
    }

    "2, THE RED LODGE, 11, ELWYN ROAD, EXMOUTH, EX8 2EL should return in the format 2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL" in {
      val osResult = resultBuilder(subBuildingName = Some("2"), buildingName = Some("THE RED LODGE"), buildingNumber = Some("11"), thoroughfareName = Some("ELWYN ROAD"), postTown = "EXMOUTH", postCode = "EX8 2EL")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL")))
      }
    }

    "40, SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN should return in the format 40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN" in {
      val osResult = resultBuilder(buildingNumber = Some("40"), thoroughfareName = Some("SKETTY PARK DRIVE"), dependentLocality = Some("SKETTY"), postTown = "SWANSEA", postCode = "SA2 8LN")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN")))
      }
    }

    "4, LYNDHURST ROAD, EXMOUTH, EX8 3DT should return in the format 4 LYNDHURST ROAD, EXMOUTH, EX8 3DT" in {
      val osResult = resultBuilder(buildingNumber = Some("4"), thoroughfareName = Some("LYNDHURST ROAD"), postTown = "EXMOUTH", postCode = "EX8 3DT")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"4 LYNDHURST ROAD, EXMOUTH, EX8 3DT")))
      }
    }

    "ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ should return in the format ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ" in {
      val osResult = resultBuilder(buildingName = Some("ASH COTTAGE"), thoroughfareName = Some("OLD BYSTOCK DRIVE"), dependentLocality = Some("BYSTOCK"), postTown = "EXMOUTH", postCode = "EX8 5EQ")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(Seq(UprnAddressPair(traderUprnValid.toString, s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ")))
      }
    }

    "return canned data for the canned postcode" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(PostcodeToAddressLookupRequest(CannedPostcode))

      whenReady(result) { r =>
        r should equal(cannedPostcodeToAddressResponse)
      }
    }

    "return organisation name in the address when one exists" in {
      val osResult = resultBuilder(organisationName = Some("DVLA"), buildingName = Some("ASH COTTAGE"), thoroughfareName = Some("OLD BYSTOCK DRIVE"), dependentLocality = Some("BYSTOCK"), postTown = "EXMOUTH", postCode = "EX8 5EQ")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r shouldBe PostcodeToAddressResponse(
          Seq(
            UprnAddressPair(
              uprn = traderUprnValid.toString,
              address = s"DVLA, ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ"
            )
          )
        )
      }
    }
  }

  "call UprnToAddressLookupRequest" should {

    "return ordnance_survey valid AddressViewModel when the uprn is valid and the OS service returns a sequence results" in {
      val osResult = resultBuilder(organisationName = Some("DVLA"), buildingName = Some("ASH COTTAGE"), thoroughfareName = Some("OLD BYSTOCK DRIVE"), dependentLocality = Some("BYSTOCK"), postTown = "EXMOUTH", postCode = "EX8 5EQ")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult ++ osResult ++ osResult))))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) { r =>
        r shouldBe UprnToAddressResponse(addressViewModel = Some(AddressViewModel(uprn = Some(traderUprnValid), address = Seq("ASH COTTAGE", "OLD BYSTOCK DRIVE", "BYSTOCK", "EXMOUTH", "EX8 5EQ"))))
      }
    }

    "return None when response status is not 200 OK" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(None)
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) { r =>
        r.addressViewModel should be(None)
      }
    }

    "return none when the uprn is valid but the OS service returns no results" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, None)))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) { r =>
        r.addressViewModel should be(None)
      }
    }

    "return none when the result has no DPA and no LPI" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) { r =>
        r.addressViewModel should be(None)
      }
    }

    "return canned data for the canned uprn" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(UprnToAddressLookupRequest(CannedUprn))

      whenReady(result) { r =>
        r should equal(cannedUprnToAddressResponse)
      }
    }

    "return without organisation name in the address even when one exists" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(resultBuilder(organisationName = Some("DVLA"), buildingName = Some("ASH COTTAGE"), thoroughfareName = Some("OLD BYSTOCK DRIVE"), dependentLocality = Some("BYSTOCK"), postTown = "EXMOUTH", postCode = "EX8 5EQ")))))
      val result = service(UprnToAddressLookupRequest(traderUprnValid))

      whenReady(result) { r =>
        r shouldBe UprnToAddressResponse(
          addressViewModel = Some(
            AddressViewModel(
              uprn = Some(traderUprnValid),
              address = Seq("ASH COTTAGE", "OLD BYSTOCK DRIVE", "BYSTOCK", "EXMOUTH", "EX8 5EQ")
            )
          )
        )
      }
    }
  }

  private final val traderUprnValid = 12345L
  private final val postcodeValid = "CM81QJ"
  private final val emptyString = ""
  private val configuration = Configuration()
  private val emptyDPAandLPI = {
    val result = Result(DPA = None, LPI = None)
    Seq(result, result, result)
  }
  private val header = Header(
    uri = new URI(""),
    offset = 0,
    totalresults = 2)
  private implicit val system = ActorSystem("LookupCommandSpecPreProduction", testConfig)

  private def testConfig: Config = {
    ConfigFactory.empty().withFallback(ConfigFactory.load())
  }

  private def resultBuilder(
                             uprn: String = traderUprnValid.toString,
                             address: String = emptyString,
                             poBoxNumber: Option[String] = None,
                             buildingName: Option[String] = None,
                             subBuildingName: Option[String] = None,
                             organisationName: Option[String] = None,
                             buildingNumber: Option[String] = None,
                             thoroughfareName: Option[String] = None,
                             dependentThoroughfareName: Option[String] = None,
                             dependentLocality: Option[String] = None,
                             postTown: String = emptyString,
                             postCode: String = emptyString
                             ) = {
    val result = Result(
      DPA = Some(
        DPA(
          UPRN = uprn,
          address = address,
          poBoxNumber = poBoxNumber,
          buildingName = buildingName,
          subBuildingName = subBuildingName,
          organisationName = organisationName,
          buildingNumber = buildingNumber,
          thoroughfareName = thoroughfareName,
          dependentThoroughfareName = dependentThoroughfareName,
          dependentLocality = dependentLocality,
          postTown = postTown,
          postCode = postCode,
          RPC = None,
          xCoordinate = 0,
          yCoordinate = 0,
          status = emptyString,
          matchScore = 0,
          matchDescription = emptyString
        )
      ),
      LPI = None)
    Seq(result)
  }

  private def lookupCommandWithCallOrdnanceSurveyStub(response: Option[Response]): AddressLookupCommand = {
    val callOrdnanceSurveyStub = mock[CallOrdnanceSurvey]
    when(callOrdnanceSurveyStub.call(any[PostcodeToAddressLookupRequest])).thenReturn(Future.successful(response))
    when(callOrdnanceSurveyStub.call(any[UprnToAddressLookupRequest])).thenReturn(Future.successful(response))
    new LookupCommand(configuration = configuration, callOrdnanceSurveyStub)
  }
}

