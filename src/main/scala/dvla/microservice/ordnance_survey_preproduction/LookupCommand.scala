package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import akka.event.Logging
import dvla.common.LogFormats
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup.{AddressDto, PostcodeToAddressLookupRequest}
import dvla.domain.ordnance_survey_preproduction.DPA
import dvla.microservice.{AddressLookupCommand, Configuration}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class LookupCommand(configuration: Configuration,
                    callOrdnanceSurvey: CallOrdnanceSurvey)
                   (implicit system: ActorSystem, executionContext: ExecutionContext)
              extends AddressLookupCommand  {

  private final val LineMaxLength = 30
  private final val TownMaxLength = 30
  private final val Separator = ", "
  private final val Space = " "
  private implicit final lazy val log = Logging(system, this.getClass)
  private implicit val AddressOrdering = new Ordering[String] {
    override def compare(x: String, y: String): Int =
      (Try(x.takeWhile(_.isDigit).toLong), Try(y.takeWhile(_.isDigit).toLong)) match {
        case (Success(xNum), Success(yNum)) => xNum.compareTo(yNum)
        case (Success(xNum), _) => -1
        case (_, Success(yNum)) => 1
        case _ => stripSameSuffix(x, y) match { case (a, b) =>
          if (x == a) a.compareTo(b)
          else compare(a, b)
      }
    }

    private def stripSameSuffix(a: String, b: String): (String, String) =
      if (a.head == b.head && !a.head.isDigit) stripSameSuffix(a.tail, b.tail)
      else (a, b)
  }

  private def applyVssAddressRules(address: DPA)(implicit trackingId: TrackingId): String =
    (address.poBoxNumber,
      address.buildingNumber,
      address.buildingName,
      address.subBuildingName,
      address.dependentThoroughfareName,
      address.thoroughfareName,
      address.dependentLocality,
      address.organisationName) match {
        case (None, None, Some(_), Some(_), None, Some(_), None, _) => {
          logMessage(trackingId, Debug, "applying rule8")
          rule8(address)
        }
        case (None, None, Some(buildingName), None, None, Some(_), _, _) if noAlphas(buildingName) => {
          logMessage(trackingId, Debug, "applying rule10")
          rule10(address)}
        case (None, Some(_), None, None, None, Some(_), _, _) => {
          logMessage(trackingId, Debug, "applying rule7")
          rule7(address)
        }
        case (Some(_), _, _, _, _, _, _, _) => {
          logMessage(trackingId, Debug, "applying rule1")
          rule1(address)
        }
        case (_, None, Some(buildingName), None, _, _, None, _) if noAlphas(buildingName) => {
          logMessage(trackingId, Debug, "applying rule9")
           rule9(address)
        }
        case (None, None, None, None, None, None, None, Some(_)) => {
          logMessage(trackingId, Debug, "applying rule11")
          rule11(address)
        }
        case (_, None, _, None, _, _, None, _) => {
          logMessage(trackingId, Debug, "applying rule2")
          rule2(address)
        }
        case (_, _, None, None, _, _, _, _) => {
          logMessage(trackingId, Debug, "applying rule3")
          rule3(address)
        }
        case (_, None, _, None, _, _, _, _) => {
          logMessage(trackingId, Debug, "applying rule4")
          rule4(address)
        }
        case (_, Some(_), _, _, _, Some(_), _, _) => {
          logMessage(trackingId, Debug, "applying rule6")
          rule6(address)
        }
        case (_, _, _, _, _, _, None, _) => {
          logMessage(trackingId, Debug, "applying rule5")
          rule5(address)
        }
        case _ => {
          logMessage(trackingId, Warn, "unable to match known rules, applying rule6 anyway...")
          rule6(address)
        }
      }

  private def addressForVss(address: DPA)(implicit trackingId: TrackingId): String = {
    val addrLines = applyVssAddressRules(address)
    if (addrLines.isEmpty) {
      val msg = s"ERROR: this address does not have any address lines - postcode: ${LogFormats.anonymize(address.postCode)}"
      throw new Exception(msg)
    }
    addrLines + Separator + parsePostTown(address.postTown) + Separator + address.postCode
  }

  //rule methods will build and return three strings for address line1, line2 and line3
  private def rule1(address: DPA): String = {
    val poBox = address.poBoxNumber.map(number => s"P.O. BOX $number")
      buildAddressLine(Seq(poBox),
          Seq(address.thoroughfareName),
          address.dependentLocality)
  }

  private def rule2(address: DPA): String =
    buildAddressLine(Seq(address.buildingName),
      Seq(address.dependentThoroughfareName),
      address.thoroughfareName)

  private def rule3(address: DPA): String =
    buildAddressLine(Seq(address.buildingNumber, address.dependentThoroughfareName),
      Seq(address.thoroughfareName),
      address.dependentLocality)

  private def rule4(address: DPA): String =
    buildAddressLine(Seq(address.buildingName, address.dependentThoroughfareName),
      Seq(address.thoroughfareName),
      address.dependentLocality)

  //NOTE rule 5 is actually redundant since the default case match would work by calling rule 6
  private def rule5(address: DPA): String =
    buildAddressLine(Seq(address.subBuildingName, address.buildingName),
      Seq(address.buildingNumber, address.dependentThoroughfareName),
      address.thoroughfareName)

  private def rule6(address: DPA): String =
    buildAddressLine(Seq(address.subBuildingName, address.buildingName),
      Seq(address.buildingNumber, address.dependentThoroughfareName, address.thoroughfareName),
      address.dependentLocality)

  private def rule7(address: DPA): String =
    buildAddressLine(Seq(address.buildingNumber, address.thoroughfareName),
      Seq(address.dependentLocality),
      None)

  private def rule8(address: DPA): String =
    buildAddressLine(Seq(address.subBuildingName),
      Seq(address.buildingName),
      address.thoroughfareName)

  private def rule9(address: DPA): String =
    buildAddressLine(Seq(address.buildingName, address.dependentThoroughfareName),
      Seq(address.thoroughfareName),
      None)

  private def rule10(address: DPA): String =
    buildAddressLine(Seq(address.buildingName, address.thoroughfareName),
      Seq(address.dependentLocality),
      None)

  private def rule11(address: DPA): String =
    buildAddressLine(Seq(address.organisationName),
      Seq.empty,
      None)

  private def noAlphas(messageText: String): Boolean =
    messageText.length==messageText.replaceAll( """[A-Za-z]""", "").length

  private def buildAddressLine(addressPart: Seq[Option[String]], line2: Seq[Option[String]], line3: Option[String]): String = {
    Seq(addressPart.flatten, line2.flatten, Seq(line3).flatten)
      .map(_.mkString(Space).trim)
      .filterNot(_.isEmpty)
      .mkString(Separator)
  }

  private def parsePostTown (rawPostTown: String): String = {
    val postTownAbbreviations = Map("LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH" -> "LLANFAIRPWLLGWYNGYLL", // LL61 5UJ
      "LETCHWORTH GARDEN CITY" -> "LETCHWORTH", // SG6 1FT
      "APPLEBY-IN-WESTMORLAND" -> "APPLEBY") // CA16 6EJ
    postTownAbbreviations.getOrElse(rawPostTown.toUpperCase, rawPostTown.take(TownMaxLength))
  }

  override def apply(request: PostcodeToAddressLookupRequest)
                           (implicit trackingId: TrackingId): Future[Seq[AddressDto]] = {
    logMessage(trackingId, Info, s"Dealing with http GET request for postcode: ${LogFormats.anonymize(request.postcode)}")
    logMessage(trackingId, Debug, s"apply - postcode: ${request.postcode}")

    def splitAddressLines(addressLines: String) = {
      logMessage(trackingId, Debug, "splitAddressLines - addressLines:" + addressLines + "<end>")
      addressLines.split(",").map(_.trim).map {line =>
        if (line.length > LineMaxLength) line.take(LineMaxLength) else line
      }.filterNot(_.isEmpty) match {
        case Array(line1) => (line1, None, None)
        case Array(line1, line2) => (line1, Some(line2), None)
        case Array(line1, line2, line3) => (line1, Some(line2), Some(line3))
        case _ =>
          // Note: No known postcode/address returned by OS that would result in reaching this but you never know...
          val msg = s"unable to extract any address lines for postcode: ${LogFormats.anonymize(request.postcode)}"
          logMessage(trackingId, Warn, msg)
          (s"", None, None)
      }}

    callOrdnanceSurvey.call(request).map { resp =>
      resp.flatMap(_.results).fold {
        // Handle no results for this postcode.
        logMessage(trackingId, Info, s"No results returned from Ordnance Survey for postcode: ${LogFormats.anonymize(request.postcode)}")
        Seq.empty[AddressDto]
      } { results =>
        val msg = "Successfully retrieved results from Ordnance Survey for postcode " +
          s"${LogFormats.anonymize(request.postcode)} - now processing those results"
        logMessage(trackingId, Info, msg)

        results.flatMap(_.DPA).map { address =>
          val addressSanitisedForVss = addressForVss(address)
          // remove org name, post town and post code from address before splitting
          val addressLinesPart = addressSanitisedForVss.replace(parsePostTown(address.postTown), "").replace(address.postCode, "").replace(address.organisationName.getOrElse(""), "")
          val (line1, line2, line3) = splitAddressLines(addressLinesPart)
          var addressLine = Seq(Some(addressSanitisedForVss)).flatten.mkString(Separator)
          if (line1.length == 0 && address.organisationName != None) {
            logMessage(trackingId, Debug, "line1 empty and org name present so set line1 to be it")

            AddressDto(
              addressLine,
              businessName = address.organisationName,
              streetAddress1 = address.organisationName.getOrElse(""),
              streetAddress2 = line2,
              streetAddress3 = line3,
              postTown = parsePostTown(address.postTown),
              postCode = address.postCode)

          } else {

            AddressDto(
              addressLine,
              businessName = address.organisationName,
              streetAddress1 = line1,
              streetAddress2 = line2,
              streetAddress3 = line3,
              postTown = parsePostTown(address.postTown),
              postCode = address.postCode
            )
          }
        }.sortBy(_.addressLine)(AddressOrdering)
      }
    } recover {
      case e: Throwable =>
        logErrorMessage(trackingId, s"Ordnance Survey postcode lookup service error", e)
        throw e
    }
  }
}
