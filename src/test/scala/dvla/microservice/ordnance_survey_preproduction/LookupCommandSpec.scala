package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup.AddressDto
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.AddressResponseDto
import dvla.domain.address_lookup.UprnToAddressLookupRequest
import dvla.domain.address_lookup.UprnToAddressResponse
import dvla.domain.ordnance_survey_preproduction.{DPA, Header, Response, Result}
import dvla.helpers.UnitSpec
import dvla.microservice.{AddressLookupCommand, Configuration}
import java.net.URI
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Second, Span}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LookupCommandSpec extends UnitSpec with MockitoSugar {
  private implicit val trackingId = TrackingId("default_tracking_id")
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
      val result = service.applyDetailedResult(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) (_ should equal(Seq(AddressDto(
        "DVLA, ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ",
        Some("DVLA"),
        "ASH COTTAGE",
        Some("OLD BYSTOCK DRIVE"),
        Some("BYSTOCK"),
        "EXMOUTH",
        postcodeValid
      ))))
    }

    "return an empty sequence when the postcode is valid but the OS service returns no results" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(Seq.empty))))
      val result = service.applyDetailedResult(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r shouldBe empty
      }
    }

    "return empty seq when response status is not 200 OK" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(None)
      val result = service.applyDetailedResult(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r => r shouldBe empty}
    }

    "return an empty sequence when the postcode is valid but the OS service returns ordnance_survey result with no DPA and no LPI" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r => r.addresses shouldBe empty}
    }

    "not throw when an address contains ordnance_survey building number that contains letters" in {
      val osResult = resultBuilder(
        buildingNumber = Some("50ABC"),
        thoroughfareName = Some("FAKE ROAD"),
        postTown = "FAKE TOWN",
        postCode = "EX8 1SN"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service.applyDetailedResult(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) (_ should equal(Seq(AddressDto(
        "50ABC FAKE ROAD, FAKE TOWN, EX8 1SN",
        None,
        "50ABC FAKE ROAD",
        None,
        None,
        "FAKE TOWN",
        postcodeValid
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
      val response = service.applyDetailedResult(PostcodeToAddressLookupRequest(postcodeValid)).futureValue

      response.map(_.addressLine.replace(", PT, PC", "")) should equal(
        Seq("1 xyz", "2/22 abc", "10 abc", "abc", "flat 3 abc", "flat 4 abc", "flat 30 abc", "xyz")
      )
    }
  }

  "call PostcodeToAddressResponse" should {
    "return ordnance_survey valid sequence of UprnAddressPairs when the postcode is valid and the OS service returns seq of results" in {
      val osResult = resultBuilder(
        organisationName = Some("DVLA"),
        buildingName = Some("ASH COTTAGE"),
        thoroughfareName = Some("OLD BYSTOCK DRIVE"),
        dependentLocality = Some("BYSTOCK"),
        postTown = "EXMOUTH",
        postCode = "EX8 5EQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult ++ osResult ++ osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result, Timeout(Span(1, Second))) { r =>
        r.addresses.foreach(a => a.uprn should equal(Some(traderUprnValid)))
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
      val osResult = resultBuilder(
        buildingNumber = Some("50ABC"),
        thoroughfareName = Some("FAKE ROAD"),
        postTown = "FAKE TOWN",
        postCode = "EX8 1SN"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"50ABC FAKE ROAD, FAKE TOWN, EX8 1SN", Some(traderUprnValid), None))
        )
      }
    }

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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"FLAT 1, 52 SALISBURY ROAD, EXMOUTH, EX8 1SN", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"FLAT 1, MONTPELLIER COURT, MONTPELLIER ROAD, EXMOUTH, EX8 1JP", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"FLAT 1, 13A, CRANLEY GARDENS, LONDON, SW7 3BB", Some(traderUprnValid), None))
        )
      }
    }

    "1/100, CRANLEY GARDENS, LONDON, SW7 3BB should return in the format 1/100 CRANLEY GARDENS, LONDON, SW7 3BB" in {
      val osResult = resultBuilder(
        buildingName = Some("1/100"),
        thoroughfareName = Some("CRANLEY GARDENS"),
        postTown = "LONDON",
        postCode = "SW7 3BB"
      )
//      val osResult = resultBuilder(buildingNumber = Some("1/100"), thoroughfareName = Some("CRANLEY GARDENS"), postTown = "LONDON", postCode = "SW7 3BB")
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"1/100 CRANLEY GARDENS, LONDON, SW7 3BB", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"UNIT 1-2, DINAN WAY TRADING ESTATE, CONCORDE ROAD, EXMOUTH, EX8 4RS", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"6 BRIXINGTON PARADE, CHURCHILL ROAD, EXMOUTH, EX8 4RJ", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"6 PARK VIEW, WOTTON LANE, LYMPSTONE, EXMOUTH, EX8 5LY", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"7 VILLA MAISON, 4 CYPRUS ROAD, EXMOUTH, EX8 2DZ", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"FLAT 1 HEATHGATE, 7 LANSDOWNE ROAD, BUDLEIGH SALTERTON, EX9 6AH", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"FLAT COURTLANDS CROSS SERVICE STATION, 397 EXETER ROAD, EXMOUTH, EX8 3NS", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"2 THE RED LODGE, 11 ELWYN ROAD, EXMOUTH, EX8 2EL", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"40 SKETTY PARK DRIVE, SKETTY, SWANSEA, SA2 8LN", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"4 LYNDHURST ROAD, EXMOUTH, EX8 3DT", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ", Some(traderUprnValid), None))
        )
      }
    }

    "PO BOX 100, BUSINESS-NAME, POST-TOWN, SN10 4TE should return in the format P.O. BOX 100, POST-TOWN, POST-CODE" in {
      val osResult = resultBuilder(
        address = "PO BOX 100, BUSINESS-NAME, DEVIZES, POST-CODE",
        organisationName = Some("BUSINESS-NAME"),
        postTown = "POST-TOWN",
        postCode = "POST-CODE",
        poBoxNumber = Some("100")
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"P.O. BOX 100, POST-TOWN, POST-CODE", Some(traderUprnValid), Some("BUSINESS-NAME")))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"1 ANOTHER ROAD, LLANFAIRPWLLGWYNGYLL, QQ9 9QQ", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"1 ANOTHER ROAD, LETCHWORTH, QQ9 9QQ", Some(traderUprnValid), None))
        )
      }
    }

    "1, ANOTHER ROAD, POST TOWN NAME IS FAR TOO LONG, QQ9 9QQ should return post town abbreviated to the first 20 characters" in {
      val osResult = resultBuilder(buildingNumber = Some("1"),
        thoroughfareName = Some("ANOTHER ROAD"),
        postTown = "POST TOWN NAME IS FAR TOO LONG",
        postCode = "QQ9 9QQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult))))
      val result = service(PostcodeToAddressLookupRequest(postcodeValid))

      whenReady(result) { r =>
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"1 ANOTHER ROAD, POST TOWN NAME IS FA, QQ9 9QQ", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"1 ANOTHER ROAD, APPLEBY, QQ9 9QQ", Some(traderUprnValid), None))
        )
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
        r.addresses.length should equal(osResult.length)
        r shouldBe PostcodeToAddressResponse(
          Seq(AddressResponseDto(s"1-9, MILLBURN ROAD, COLERAINE, BT52 1QS", Some(traderUprnValid), Some("J K C SPECIALIST CARS LTD")))
        )
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
        r shouldBe PostcodeToAddressResponse(
          Seq(
            AddressResponseDto(s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ", Some(traderUprnValid), Some("DVLA"))
          )
        )
      }
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
      val response = service(PostcodeToAddressLookupRequest(postcode = postcodeValid)).futureValue

      response.addresses.map{_.address.replace(", PT, PC", "")} should equal(
        Seq("1 xyz", "2/22 abc", "10 abc", "abc", "flat 3 abc", "flat 4 abc", "flat 30 abc", "xyz")
      )
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
        r shouldBe PostcodeToAddressResponse(
          Seq(
            AddressResponseDto(s"ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ", Some(traderUprnValid), Some("DVLA"))
          )
        )
      }
    }
  }

  "call UprnToAddressLookupRequest" should {
    "return ordnance_survey valid AddressViewModel when the uprn is valid and the OS service returns a sequence results" in {
      val osResult = resultBuilder(
        organisationName = Some("DVLA"),
        buildingName = Some("ASH COTTAGE"),
        thoroughfareName = Some("OLD BYSTOCK DRIVE"),
        dependentLocality = Some("BYSTOCK"),
        postTown = "EXMOUTH",
        postCode = "EX8 5EQ"
      )
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(osResult ++ osResult ++ osResult))))
      val result = service(UprnToAddressLookupRequest(numericTraderUprnValid))


      whenReady(result) { r =>
        r shouldBe UprnToAddressResponse(addressViewModel =
          Some(AddressViewModel(
            uprn = Some(numericTraderUprnValid),
            address = Seq("ASH COTTAGE", "OLD BYSTOCK DRIVE", "BYSTOCK", "EXMOUTH", "EX8 5EQ"))
          )
        )
      }
    }

    "return None when response status is not 200 OK" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(None)
      val result = service(UprnToAddressLookupRequest(numericTraderUprnValid))

      whenReady(result) { r =>
        r.addressViewModel should be(None)
      }
    }

    "return none when the uprn is valid but the OS service returns no results" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, None)))
      val result = service(UprnToAddressLookupRequest(numericTraderUprnValid))

      whenReady(result) { r =>
        r.addressViewModel should be(None)
      }
    }

    "return none when the result has no DPA and no LPI" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(Response(header, Some(emptyDPAandLPI))))
      val result = service(UprnToAddressLookupRequest(numericTraderUprnValid))

      whenReady(result) { r =>
        r.addressViewModel should be(None)
      }
    }

    "return without organisation name in the address even when one exists" in {
      val service = lookupCommandWithCallOrdnanceSurveyStub(Some(
        Response(
          header,
          Some(
            resultBuilder(
              organisationName = Some("DVLA"),
              buildingName = Some("ASH COTTAGE"),
              thoroughfareName = Some("OLD BYSTOCK DRIVE"),
              dependentLocality = Some("BYSTOCK"),
              postTown = "EXMOUTH",
              postCode = "EX8 5EQ")
          )
        )
      ))
      val result = service(UprnToAddressLookupRequest(numericTraderUprnValid))

      whenReady(result) { r =>
        r shouldBe UprnToAddressResponse(
          addressViewModel = Some(
            AddressViewModel(
              uprn = Some(numericTraderUprnValid),
              address = Seq("ASH COTTAGE", "OLD BYSTOCK DRIVE", "BYSTOCK", "EXMOUTH", "EX8 5EQ")
            )
          )
        )
      }
    }
  }

//  private final val traderUprnValid = 12345L
  private final val numericTraderUprnValid = 12345L

//  private final val traderUprnValid = "ASH COTTAGE, OLD BYSTOCK DRIVE, BYSTOCK, EXMOUTH, EX8 5EQ"
  private final val traderUprnValid = "12345"

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
//  private implicit val trackingId = TrackingId("default_tracking_id")
  private def testConfig: Config = {
    ConfigFactory.empty().withFallback(ConfigFactory.load())
  }

  private def resultBuilder(uprn: String = traderUprnValid,
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
    UPRN =  traderUprnValid,
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

  private def lookupCommandWithCallOrdnanceSurveyStub(response: Option[Response]): AddressLookupCommand = {
    val callOrdnanceSurveyStub = mock[CallOrdnanceSurvey]
    when(callOrdnanceSurveyStub.call(any[PostcodeToAddressLookupRequest])).thenReturn(Future.successful(response))
    when(callOrdnanceSurveyStub.call(any[UprnToAddressLookupRequest])).thenReturn(Future.successful(response))
    new LookupCommand(configuration = configuration, callOrdnanceSurveyStub)
  }
}
