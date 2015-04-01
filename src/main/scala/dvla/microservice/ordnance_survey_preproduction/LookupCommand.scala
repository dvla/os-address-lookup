package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import akka.event.Logging
import dvla.common.LogFormats
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.UprnToAddressLookupRequest
import dvla.domain.address_lookup.UprnToAddressResponse
import dvla.domain.ordnance_survey_preproduction.{DPA, Response}
import dvla.microservice.ordnance_survey_preproduction.LookupCommand.CannedPostcode
import dvla.microservice.ordnance_survey_preproduction.LookupCommand.cannedPostcodeToAddressResponse
import dvla.microservice.ordnance_survey_preproduction.LookupCommand.CannedUprn
import dvla.microservice.ordnance_survey_preproduction.LookupCommand.cannedUprnToAddressResponse
import dvla.microservice.{AddressLookupCommand, Configuration}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class LookupCommand(configuration: Configuration,
                    callOrdnanceSurvey: CallOrdnanceSurvey)
                   (implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {

  private final val Separator = ", "
  private final val Nothing = ""
  private final val Space = " "
  private final lazy val log = Logging(system, this.getClass)

  private def addresses(postcode: String, resp: Option[Response], showBusinessName: Option[Boolean]): Seq[UprnAddressPair] = {
    val responseResults = resp.flatMap(_.results)

    responseResults match {
      case Some(results) =>
        val addresses = results.flatMap(_.DPA)
        log.info(s"Returning result for postcode request ${LogFormats.anonymize(postcode)}")
        // sort them before translating. otherwise if they have a business name the order will be different from
        // the previous call.
        val addrs = addresses.sortBy(r => r.address)
        addrs map { address =>
          val addressAsString = {
            val addressSanitisedForVss = applyVssRules(address)
            (showBusinessName, address.organisationName) match {
              case (Some(show), Some(organisationName)) if show =>
                organisationName + Separator + addressSanitisedForVss
              case _ =>
                addressSanitisedForVss
            }
          }
          UprnAddressPair(address.UPRN, addressAsString)
        }
      case None =>
        // Handle no results for this postcode.
        log.info(s"No results returned for postcode: ${LogFormats.anonymize(postcode)}")
        Seq.empty
    }
  }

  private def applyVssRules(address: DPA): String = {
    val addressLines =
      (address.poBoxNumber,
        address.buildingNumber,
        address.buildingName,
        address.subBuildingName,
        address.dependentThoroughfareName,
        address.thoroughfareName,
        address.dependentLocality) match {
        case (None, None, Some(_), Some(_), None, Some(_), None) => rule8(address)
        case (None, Some(_), None, None, None, Some(_), _) => rule7(address)
        case (Some(_), _, _, _, _, _, _) => rule1(address)
        case (_, None, _, None, _, _, None) => rule2(address)
        case (_, _, None, None, _, _, _) => rule3(address)
        case (_, None, _, None, _, _, _) => rule4(address)
        case (_, Some(_), _, _, _, Some(_), _) => rule6(address)
        case (_, _, _, _, _, _, None) => rule5(address)
        case _ => rule6(address)
      }
    addressLines + buildPostTown(address.postTown) + Separator + address.postCode
  }

  //rule methods will build and return three strings for address line1, line2 and line3
  private def rule1(address: DPA): String = {
    val poBox = address.poBoxNumber.map(number => s"P.O. BOX $number")
    lineBuild(Seq(poBox)) +
      lineBuild(Seq(address.thoroughfareName)) +
      lineBuild(Seq(address.dependentLocality))
  }

  private def rule2(address: DPA): String =
    lineBuild(Seq(address.buildingName)) +
      dependentThoroughfareNameNotBlank(address) +
      lineBuild(Seq(address.thoroughfareName))

  private def dependentThoroughfareNameNotBlank(address: DPA): String =
    address.dependentThoroughfareName match {
      case Some(dependentThoroughfareName) => lineBuild(Seq(address.dependentThoroughfareName))
      case _ => ""
    }

  private def rule3(address: DPA): String =
    lineBuild(Seq(address.buildingNumber, address.dependentThoroughfareName)) +
      lineBuild(Seq(address.thoroughfareName)) +
      lineBuild(Seq(address.dependentLocality))

  private def rule4(address: DPA): String =
    lineBuild(Seq(address.buildingName, address.dependentThoroughfareName)) +
      lineBuild(Seq(address.thoroughfareName)) +
      lineBuild(Seq(address.dependentLocality))

  private def rule5(address: DPA): String =
    lineBuild(Seq(address.subBuildingName, address.buildingName)) +
      lineBuild(Seq(address.buildingNumber, address.dependentThoroughfareName)) +
      lineBuild(Seq(address.thoroughfareName))

  private def rule6(address: DPA): String =
    lineBuild(Seq(address.subBuildingName, address.buildingName)) +
      lineBuild(Seq(address.buildingNumber, address.dependentThoroughfareName, address.thoroughfareName)) +
      lineBuild(Seq(address.dependentLocality))

  private def rule7(address: DPA): String =
    lineBuild(Seq(address.buildingNumber, address.thoroughfareName)) +
      lineBuild(Seq(address.dependentLocality)) +
      Nothing

  private def rule8(address: DPA): String =
    lineBuild(Seq(address.subBuildingName)) +
      lineBuild(Seq(address.buildingName)) +
      lineBuild(Seq(address.thoroughfareName))

  @tailrec
  private def lineBuild(addressPart: Seq[Option[String]], accumulatedLine: String = Nothing): String = {
    if (addressPart.size == 1) lastItemInList(addressPart.head, accumulatedLine, Separator)
    else lineBuild(addressPart.tail, accumulateLine(addressPart.head, accumulatedLine, Space))
  }

  private def accumulateLine(currentItem: Option[String], accumulatedLine: String, lastChar: String): String = {
    currentItem match {
      case Some(item) => accumulatedLine + item + lastChar
      case _ => accumulatedLine
    }
  }

  private def lastItemInList(lastItem: Option[String], accumulatedLine: String, lastChar: String): String = {
    val currentAddressLine = if (accumulatedLine.takeRight(1) == Space) accumulatedLine.dropRight(1) else accumulatedLine
    lastItem match {
      case Some(item) => accumulatedLine + item + lastChar
      case _ => if (currentAddressLine + lastChar == lastChar) Nothing
      else currentAddressLine + lastChar
    }
  }

  private def buildPostTown (rawPostTown: String): String = {
    val postTownAbbreviations = Map("LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH" -> "LLANFAIRPWLLGWYNGYLL", // LL61 5UJ
                               "LETCHWORTH GARDEN CITY" -> "LETCHWORTH", // SG6 1FT
                               "APPLEBY-IN-WESTMORLAND" -> "APPLEBY") // CA16 6EJ
    postTownAbbreviations.getOrElse(rawPostTown.toUpperCase, rawPostTown.take(20))
  }

  private def address(uprn: Long, resp: Option[Response]): Option[AddressViewModel] = {
    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap(_.DPA)
        log.info(s"Returning result for uprn request ${LogFormats.anonymize(uprn.toString)}")
        require(addresses.length >= 1, s"Should be at least one address for the UPRN")
        Some(AddressViewModel(
          uprn = Some(addresses.head.UPRN.toLong),
          address = applyVssRules(addresses.head).split(", ")
        )) // Translate to view model.
      case None =>
        log.info(s"No results returned by web service for submitted UPRN: ${LogFormats.anonymize(uprn.toString)}")
        None
    }
  }

  override def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = {
    log.info(s"Dealing with the post request for postcode ${LogFormats.anonymize(request.postcode)}")

    if (request.postcode == CannedPostcode)
      Future(cannedPostcodeToAddressResponse)
    else
      callOrdnanceSurvey.call(request).map { resp =>
        PostcodeToAddressResponse(addresses(request.postcode, resp, request.showBusinessName))
      }.recover {
        case e: Throwable =>
          log.info(s"Ordnance Survey postcode lookup service error: ${e.toString}")
          PostcodeToAddressResponse(Seq.empty)
      }
  }

  override def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = {
    log.info(s"Dealing with the post request for uprn ${LogFormats.anonymize(request.uprn.toString)}")

    if (request.uprn == CannedUprn)
      Future(cannedUprnToAddressResponse)
    else
      callOrdnanceSurvey.call(request).map { resp =>
        UprnToAddressResponse(address(request.uprn, resp))
      }.recover {
        case e: Throwable =>
          log.info(s"Ordnance Survey uprn lookup service error: ${e.toString}")
          UprnToAddressResponse(None)
      }
  }
}

object LookupCommand {

  // Must be in a valid format yet not exist.
  private[ordnance_survey_preproduction] final val CannedPostcode = "QQ99QQ"
  private[ordnance_survey_preproduction] final val CannedAddress = "Not real street, Not real town, QQ9 9QQ"
  private[ordnance_survey_preproduction] final val CannedUprn = 999999999999L
  private[ordnance_survey_preproduction] val cannedPostcodeToAddressResponse =
    PostcodeToAddressResponse(addresses = Seq(UprnAddressPair(CannedUprn.toString, CannedAddress)))
  private[ordnance_survey_preproduction] val cannedUprnToAddressResponse =
    UprnToAddressResponse(Some(AddressViewModel(
      uprn = Some(CannedUprn),
      address = Seq("Not real street", "Not real town", CannedPostcode)
    )))
}