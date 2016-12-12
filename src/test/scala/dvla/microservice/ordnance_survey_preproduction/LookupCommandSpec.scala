package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup.{AddressDto, PostcodeToAddressLookupRequest}
import java.net.URI

import dvla.domain.ordnance_survey_preproduction.{DPA, Header, Response, Result}
import dvla.helpers.UnitSpec
import dvla.microservice.{AddressLookupCommand, Configuration}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import spray.can.Http.ConnectionException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LookupCommandSpec extends UnitSpec with MockitoSugar {

  private implicit val trackingId = TrackingId("test-tracking-id")

  "Looking up addresses by postcode with details" should {
    "return valid Addresses when the postcode is valid and the OS service returns seq of results" in {
      val osResult = resultBuilder(
        organisationName = Some("DVLA"),
        buildingName = Some("ASH COTTAGE"),
        thoroughfareName = Some("OLD BYSTOCK DRIVE"),
        dependentLocality = Some("BYSTOCK"),
        postTown = "EXMOUTH",
        postCode = "EX8 5EQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) (_ should equal(Seq(AddressDto(
        "ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ",
        Some("DVLA"),
        "ASH COTTAGE",
        Some("OLD BYSTOCK DRIVE"),
        Some("BYSTOCK"),
        "EXMOUTH",
        "EX8 5EQ"
      ))))
    }

    "return an empty sequence when the postcode is valid but the OS service returns no results" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(Seq.empty))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r shouldBe empty
      }
    }

    "return empty seq when response status is not 200 OK" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(None)
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r => r shouldBe empty}
    }

    "return an empty sequence when the postcode is valid but the OS service returns ordnance_survey result with no DPA and no LPI" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r => r shouldBe empty}
    }

    "not throw when an address contains ordnance_survey building number that contains letters" in {
      val osResult = resultBuilder(
        buildingNumber = Some("50ABC"),
        thoroughfareName = Some("FAKE ROAD"),
        postTown = "FAKE TOWN",
        postCode = "EX8 1SN"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) (_ should equal(Seq(AddressDto(
        "50ABC FAKE ROAD, FAKE TOWN, EX8 1SN",
        None,
        "50ABC FAKE ROAD",
        None,
        None,
        "FAKE TOWN",
        "EX8 1SN"
      ))))
    }

    "sort the result by number first and then alphabetically" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(rList(
        (None, "xyz"),
        (None, "abc"),
        (Some("2/22"), "abc"),
        (Some("flat 30"), "abc"),
        (Some("flat 3"), "abc"),
        (Some("flat 4"), "abc"),
        (Some("10"), "abc"),
        (Some("1"), "xyz")
      )))
      val response = service(PostcodeToAddressLookupRequest(postcodeValid)).futureValue

      response.map(_.addressLine.replace(", PT, PC", "")) should equal(
        Seq("1 xyz", "2/22 abc", "10 abc", "abc", "flat 3 abc", "flat 4 abc", "flat 30 abc", "xyz")
      )
    }

    "throw an exception when the service cannot connect to the OS end point" in {
      val callOrdnanceSurveyMock = mock[CallOrdnanceSurvey]
      when(callOrdnanceSurveyMock.call(any[PostcodeToAddressLookupRequest])(any[TrackingId]))
        .thenReturn(Future.failed(new ConnectionException("Connection attempt to non-existent-endpoint.co.uk:443 failed")))
      val command = new LookupCommand(configuration = configuration, callOrdnanceSurveyMock)

      val result = command(PostcodeToAddressLookupRequest(postcodeValid))
      whenReady(result.failed) { e =>
        e shouldBe a [ConnectionException]
      }
    }

  }

  "call lookup command" should {
    "FLAT 1, 52, SALISBURY ROAD, EXMOUTH, EX8 1SN should return in the format FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN" in {
      val osResult = resultBuilder(
        subBuildingName = Some("FLAT 1"),
        buildingNumber = Some("52"),
        thoroughfareName = Some("SALISBURY ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 1SN"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN",
          None,
          s"FLAT 1",
          Some(s"52 SALISBURY ROAD"),
          None,
          s"EXMOUTH",
          s"EX8 1SN"))
      }
    }

    "FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP should return in the format FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP" in {
      val osResult = resultBuilder(
        subBuildingName = Some("FLAT 1"),
        buildingName = Some("MONTPELLIER COURT"),
        thoroughfareName = Some("MONTPELLIER ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 1JP"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP", 
          None,
          s"FLAT 1",
          Some(s"MONTPELLIER COURT"),
          Some(s"MONTPELLIER ROAD"),
          s"EXMOUTH",
          s"EX8 1JP"))
      }
    }

    "FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB should return in the format FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB" in {
      val osResult = resultBuilder(
        subBuildingName = Some("FLAT 1"),
        buildingName = Some("13A"),
        thoroughfareName = Some("CRANLEY GARDENS"),
        postTown = "LONDON",
        postCode = "SW7 3BB"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB", 
          None,
          s"FLAT 1",
          Some("13A"),
          Some("CRANLEY GARDENS"),
          s"LONDON",
          s"SW7 3BB"))
      }
    }

    "1/100, CRANLEY GARDENS, LONDON, SW7 3BB should return in the format 1/100 CRANLEY GARDENS, LONDON, SW7 3BB" in {
      val osResult = resultBuilder(
        buildingName = Some("1/100"),
        thoroughfareName = Some("CRANLEY GARDENS"),
        postTown = "LONDON",
        postCode = "SW7 3BB"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"1/100 CRANLEY GARDENS, LONDON, SW7 3BB",
          None,
          s"1/100 CRANLEY GARDENS",
          None,
          None,
          s"LONDON",
          s"SW7 3BB"))
      }
    }

    "UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS should return in the format UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS" in {
      val osResult = resultBuilder(
        buildingName = Some("UNIT 1-2"),
        dependentThoroughfareName = Some("DINAN WAY TRADING ESTATE"),
        thoroughfareName = Some("CONCORDE ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 4RS"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS",
          None,
          s"UNIT 1-2",
          Some("DINAN WAY TRADING ESTATE"),
          Some("CONCORDE ROAD"),
          s"EXMOUTH",
          s"EX8 4RS"))
      }
    }

    "6, BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ should return in the format 6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ" in {
      val osResult = resultBuilder(
        buildingNumber = Some("6"),
        dependentThoroughfareName = Some("BRIXINGTON PARADE"),
        thoroughfareName = Some("CHURCHILL ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 4RJ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ",
          None,
          s"6 BRIXINGTON PARADE",
          Some("CHURCHILL ROAD"),
          None,
          s"EXMOUTH",
          s"EX8 4RJ"))
      }
    }

    "6, PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY should return in the format 6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY" in {
      val osResult = resultBuilder(
        buildingNumber = Some("6"),
        dependentThoroughfareName = Some("PARK VIEW"),
        thoroughfareName = Some("WOTTON LANE"),
        dependentLocality = Some("LYMPSTONE"),
        postTown = "EXMOUTH",
        postCode = "EX8 5LY"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY",
          None,
          s"6 PARK VIEW",
          Some("WOTTON LANE"),
          Some("LYMPSTONE"),
          s"EXMOUTH",
          s"EX8 5LY"))
      }
    }

    "7, VILLA MAISON, 4, CYPRUS ROAD, EXMOUTH, EX8 2DZ should return in the format 7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ" in {
      val osResult = resultBuilder(
        subBuildingName = Some("7"),
        buildingName = Some("VILLA MAISON"),
        buildingNumber = Some("4"),
        thoroughfareName = Some("CYPRUS ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 2DZ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ",
          None,
          s"7 VILLA MAISON",
          Some("4 CYPRUS ROAD"),
          None,
          s"EXMOUTH",
          s"EX8 2DZ"))
      }
    }

    "FLAT 1,HEATHGATE,7,LANSDOWNE ROAD,BUDLEIGH SALTERTON,EX9 6AH should return in the format FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH" in {
      val osResult = resultBuilder(
        subBuildingName = Some("FLAT 1"),
        buildingName = Some("HEATHGATE"),
        buildingNumber = Some("7"),
        thoroughfareName = Some("LANSDOWNE ROAD"),
        postTown = "BUDLEIGH SALTERTON",
        postCode = "EX9 6AH"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH",
          None,
          s"FLAT 1 HEATHGATE",
          Some("7 LANSDOWNE ROAD"),
          None,
          s"BUDLEIGH SALTERTON",
          s"EX9 6AH"))
      }
    }

    "FLAT, COURTLANDS CROSS SERVICE STATION, 397, EXETER ROAD, EXMOUTH, EX8 3NS should return in the format FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS" in {
      val osResult = resultBuilder(
        subBuildingName = Some("FLAT"),
        buildingName = Some("COURTLANDS CROSS SERVICE STATION"),
        buildingNumber = Some("397"),
        thoroughfareName = Some("EXETER ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 3NS"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS",
          None,
          s"FLAT COURTLANDS CROSS SERVICE ",  // NOTE the truncation here
          Some("397 EXETER ROAD"),
          None,
          s"EXMOUTH",
          s"EX8 3NS"))
      }
    }

    "2, THE RED LODGE, 11, ELWYN ROAD, EXMOUTH, EX8 2EL should return in the format 2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL" in {
      val osResult = resultBuilder(
        subBuildingName = Some("2"),
        buildingName = Some("THE RED LODGE"),
        buildingNumber = Some("11"),
        thoroughfareName = Some("ELWYN ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 2EL"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL",
          None,
          s"2 THE RED LODGE",
          Some("11 ELWYN ROAD"),
          None,
          s"EXMOUTH",
          s"EX8 2EL"))
      }
    }

    "40, SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN should return in the format 40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN" in {
      val osResult = resultBuilder(
        buildingNumber = Some("40"),
        thoroughfareName = Some("SKETTY PARK DRIVE"),
        dependentLocality = Some("SKETTY"),
        postTown = "SWANSEA",
        postCode = "SA2 8LN"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN",
          None,
          s"40 SKETTY PARK DRIVE",
          Some("SKETTY"),
          None,
          s"SWANSEA",
          s"SA2 8LN"))
      }
    }

    "4, LYNDHURST ROAD, EXMOUTH, EX8 3DT should return in the format 4 LYNDHURST ROAD, EXMOUTH, EX8 3DT" in {
      val osResult = resultBuilder(
        buildingNumber = Some("4"),
        thoroughfareName = Some("LYNDHURST ROAD"),
        postTown = "EXMOUTH",
        postCode = "EX8 3DT"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"4 LYNDHURST ROAD, EXMOUTH, EX8 3DT",
          None,
          s"4 LYNDHURST ROAD",
          None,
          None,
          s"EXMOUTH",
          s"EX8 3DT"))
      }
    }

    "ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ should return in the format ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ" in {
      val osResult = resultBuilder(
        buildingName = Some("ASH COTTAGE"),
        thoroughfareName = Some("OLD BYSTOCK DRIVE"),
        dependentLocality = Some("BYSTOCK"),
        postTown = "EXMOUTH",
        postCode = "EX8 5EQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ",
          None,
          s"ASH COTTAGE",
          Some("OLD BYSTOCK DRIVE"),
          Some("BYSTOCK"),
          s"EXMOUTH",
          s"EX8 5EQ"))
      }
    }

    "PO BOX 100, BUSINESS-NAME, POST-TOWN, SN10 4TE should return in the format P.O. BOX 100, POST-TOWN, POST-CODE" in {
      val osResult = resultBuilder(
        address = "P.O. BOX 100, BUSINESS-NAME, DEVIZES, POST-CODE",
        organisationName = Some("BUSINESS-NAME"),
        postTown = "POST-TOWN",
        postCode = "POST-CODE",
        poBoxNumber = Some("100")
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"P.O. BOX 100, POST-TOWN, POST-CODE",
          Some("BUSINESS-NAME"),
          s"P.O. BOX 100",
          None,
          None,
          s"POST-TOWN",
          s"POST-CODE"))
      }
    }

    "1, ANOTHER ROAD, LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH, QQ9 9QQ should return post town in the format LLANFAIRPWLLGWYNGYLL" in {
      val osResult = resultBuilder(
        buildingNumber = Some("1"),
        thoroughfareName = Some("ANOTHER ROAD"),
        postTown = "LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH",
        postCode = "QQ9 9QQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"1 ANOTHER ROAD, LLANFAIRPWLLGWYNGYLL, QQ9 9QQ",
          None,
          s"1 ANOTHER ROAD",
          None,
          None,
          s"LLANFAIRPWLLGWYNGYLL",
          s"QQ9 9QQ"))
      }
    }

    "1, ANOTHER ROAD, LETCHWORTH GARDEN CITY, QQ9 9QQ should return post town in the format LETCHWORTH" in {
      val osResult = resultBuilder(
        buildingNumber = Some("1"),
        thoroughfareName = Some("ANOTHER ROAD"),
        postTown = "LETCHWORTH GARDEN CITY",
        postCode = "QQ9 9QQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"1 ANOTHER ROAD, LETCHWORTH, QQ9 9QQ",
          None,
          s"1 ANOTHER ROAD",
          None,
          None,
          s"LETCHWORTH",
          s"QQ9 9QQ"))
      }
    }

    "1, ANOTHER ROAD, POST TOWN NAME IS FAR TOO LONG AND IS OVER THIRTY CHARACTERS, QQ9 9QQ should return post town abbreviated to the first 30 characters" in {
      val osResult = resultBuilder(buildingNumber = Some("1"),
        thoroughfareName = Some("ANOTHER ROAD"),
        postTown = "POST TOWN NAME IS FAR TOO LONG AND IS OVER THIRTY CHARACTERS",
        postCode = "QQ9 9QQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"1 ANOTHER ROAD, POST TOWN NAME IS FAR TOO LONG, QQ9 9QQ",
          None,
          s"1 ANOTHER ROAD",
          None,
          None,
          s"POST TOWN NAME IS FAR TOO LONG",
          s"QQ9 9QQ"))
      }
    }

    "1, ANOTHER ROAD, APPLEBY-IN-WESTMORLAND should return post town in the format APPLEBY" in {
      val osResult = resultBuilder(
        buildingNumber = Some("1"),
        thoroughfareName = Some("ANOTHER ROAD"),
        postTown = "APPLEBY-IN-WESTMORLAND",
        postCode = "QQ9 9QQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"1 ANOTHER ROAD, APPLEBY, QQ9 9QQ",
          None,
          s"1 ANOTHER ROAD",
          None,
          None,
          s"APPLEBY",
          s"QQ9 9QQ"))
      }
    }

    "J K C SPECIALIST CARS LTD, 1-9, , MILLBURN ROAD, COLERAINE, BT52 1QS should return in the format 1-9, MILLBURN ROAD, COLERAINE, BT52 1QS" in {
      val osResult = resultBuilder(
        buildingName = Some("1-9"),
        dependentThoroughfareName = Some(" "),
        organisationName = Some("J K C SPECIALIST CARS LTD"),
        thoroughfareName = Some("MILLBURN ROAD"),
        postTown = "COLERAINE",
        postCode = "BT52 1QS"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"1-9, MILLBURN ROAD, COLERAINE, BT52 1QS",
          Some("J K C SPECIALIST CARS LTD"),
          s"1-9",
          Some("MILLBURN ROAD"),
          None,
          s"COLERAINE",
          s"BT52 1QS"))
      }
    }

    "return without organisation name in the address when one exists but we don't specify whether to show it" in {
      val osResult = resultBuilder(
        organisationName = Some("DVLA"),
        buildingName = Some("ASH COTTAGE"),
        thoroughfareName = Some("OLD BYSTOCK DRIVE"),
        dependentLocality = Some("BYSTOCK"),
        postTown = "EXMOUTH",
        postCode = "EX8 5EQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcode = postcodeValid))

      whenReady(result) { r =>
        r shouldBe Seq(AddressDto(s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ",
          Some("DVLA"),
          s"ASH COTTAGE",
          Some("OLD BYSTOCK DRIVE"),
          Some("BYSTOCK"),
          s"EXMOUTH",
          s"EX8 5EQ"))
      }
    }

    "do not return organisation name in the address when one exists but we specify not to show it" in {
      val osResult = resultBuilder(
        organisationName = Some("DVLA"),
        buildingName = Some("ASH COTTAGE"),
        thoroughfareName = Some("OLD BYSTOCK DRIVE"),
        dependentLocality = Some("BYSTOCK"),
        postTown = "EXMOUTH",
        postCode = "EX8 5EQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcode = postcodeValid))

      whenReady(result) { r =>
        r shouldBe Seq(AddressDto(s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ",
          Some("DVLA"),
          s"ASH COTTAGE",
          Some("OLD BYSTOCK DRIVE"),
          Some("BYSTOCK"),
          s"EXMOUTH",
          s"EX8 5EQ"))
      }
    }

    "handle addresses that are returned from ordnance survey with only organisation name, post town and postcode in address line" in {
      val osResult = resultBuilder(
        organisationName = Some("ROYAL NAVY"),
        postTown = "PLYMOUTH",
        postCode = "PL2 2BG"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest("PL22BG"))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"ROYAL NAVY, PLYMOUTH, PL2 2BG",
          Some("ROYAL NAVY"),
          s"ROYAL NAVY",
          None,
          None,
          s"PLYMOUTH",
          s"PL2 2BG"))
      }
    }

    "exercise rule5 - everything bar dependentLocality, PO box and thoroughfareName" in {
      val osResult = resultBuilder(
        address = "SMELLY SHACK, 1 YET, PLYMOUTH, RL5 1TS",
        //None po box required to get past rule1 match
        buildingName = Some("SHACK"), // L1
        subBuildingName = Some("SMELLY"), // L1
        organisationName = Some("ROYAL NAVY"),
        buildingNumber = Some("1"), // L2
        //NOTE although rule5 includes buildingNumber and thoroughfareName, one or both must be None to get past rule6 match
        //thoroughfareName = Some("ANOTHER ROAD"), // L3
        dependentThoroughfareName = Some("YET"), // L2
        //None dependentLocality to match rule5
        postTown = "PLYMOUTH",
        postCode = "RL5 1TS"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest("RL5 1TS"))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"SMELLY SHACK, 1 YET, PLYMOUTH, RL5 1TS",
          Some("ROYAL NAVY"),
          s"SMELLY SHACK",
          Some("1 YET"),
          None, // should be "ANOTHER ROAD"
          s"PLYMOUTH",
          s"RL5 1TS"))
      }
    }

    "exercise default rule6" in {
      val osResult = resultBuilder(
        address = "SMELLY SHACK, 1 YET, ALOCALITY, PLYMOUTH, RL6 1TS",
        //None po box required to get past rule1 match
        buildingName = Some("SHACK"), // L1
        subBuildingName = Some("SMELLY"), // L1
        buildingNumber = Some("1"), // L2
        //None buildingNumber/thoroughfareName required to get past rule6 match
        dependentThoroughfareName = Some("YET"), // L2
        dependentLocality = Some("ALOCALITY"), // to match default rule6
        postTown = "PLYMOUTH",
        postCode = "RL6 1TS"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest("RL6 1TS"))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"SMELLY SHACK, 1 YET, ALOCALITY, PLYMOUTH, RL6 1TS",
          None,
          s"SMELLY SHACK",
          Some("1 YET"),
          Some("ALOCALITY"),
          s"PLYMOUTH",
          s"RL6 1TS"))
      }
    }

    "throw an exception when no address lines can be extracted" in {
      val osResult = resultBuilder(
        address = "PLYMOUTH, XX6 1XX",
        postTown = "PLYMOUTH",
        postCode = "XX6 1XX"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest("XX6 1XX"))

      whenReady(result.failed) { e =>
        e shouldBe a [Exception]
        e.getMessage should include("address does not have any address lines")
      }
    }

    "exercise buildAddressLine with empty first parameter/line1 via rule8" in {
      val osResult = resultBuilder(
        address = "TRUMP TOWER, DOWN TOWN, PLYMOUTH, RL6 1ZZ",
        subBuildingName = Some(" "),
        buildingName = Some("TRUMP TOWER"),
        thoroughfareName = Some("DOWN TOWN"),
        postTown = "PLYMOUTH",
        postCode = "RL6 1ZZ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest("RL6 1ZZ"))

      whenReady(result) { r =>
        r.length should equal(osResult.length)
        r shouldBe Seq(AddressDto(s"TRUMP TOWER, DOWN TOWN, PLYMOUTH, RL6 1ZZ",
          None,
          s"TRUMP TOWER",
          Some("DOWN TOWN"),
          None,
          s"PLYMOUTH",
          s"RL6 1ZZ"))
      }
    }

    //NOTE Post town and building name (in addition to post code) only should never be returned by OS Places query so no need to mock/test
  }

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

  private def resultBuilder(uprn: String = "",
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
                            postCode: String = emptyString) = {
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
          postCode = postCode
        )
      ),
      LPI = None)
    Seq(result)
  }

  private val dpa = DPA(
    UPRN =  "",
    address = emptyString,
    poBoxNumber = None,
    buildingName = None,
    subBuildingName = None,
    organisationName = None,
    buildingNumber = None,
    thoroughfareName = None,
    dependentThoroughfareName = None,
    dependentLocality = None,
    postTown = "PT",
    postCode = "PC"
  )

  private def r(num: Option[String], thoroughfareName: String) =
    Result(Some(dpa.copy(buildingNumber = num, thoroughfareName = Some(thoroughfareName))), None)
  private def rList(address: (Option[String], String)*) =
    Response(header, Some(address.map{ case(num, addr) =>
     r(num, addr)
    }))

  private def lookupCommandWithCallOrdnanceSurveyStub(response: Option[Response],
                                                      configuration: Configuration = configuration): AddressLookupCommand = {
    val callOrdnanceSurveyStub = mock[CallOrdnanceSurvey]
    when(callOrdnanceSurveyStub.call(any[PostcodeToAddressLookupRequest])(any[TrackingId]))
      .thenReturn(Future.successful(response))
    new LookupCommand(configuration = configuration, callOrdnanceSurveyStub)
  }
}
