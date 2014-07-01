package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import akka.event.Logging
import dvla.common.LogFormats
import dvla.domain.address_lookup._
import dvla.domain.ordnance_survey_preproduction.Response
import dvla.microservice.{AddressLookupCommand, Configuration}
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse, StatusCodes}
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import scala.annotation.tailrec
import scala.concurrent._
import LookupCommand._

class LookupCommand(override val configuration: Configuration,
                    val postcodeUrlBuilder: PostcodeUrlBuilder,
                    val uprnUrlBuilder: UprnUrlBuilder)(implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {
  private final val separator = ", "
  private final val nothing = ""
  private final val space = " "
  private final lazy val log = Logging(system, this.getClass)

  private def buildUprnAddressPairSeq(postcode: String, resp: Option[Response]): Seq[UprnAddressPair] = {
    val responseResults = resp.flatMap(_.results)

    responseResults match {
      case Some(results) =>
        val addresses = results.flatMap(_.DPA)
        log.info(s"Returning result for postcode request ${LogFormats.anonymize(postcode)}")
        val uprnAddressPairs = addresses map {
          address => UprnAddressPair(address.UPRN,
            addressRulePicker(address.poBoxNumber, address.buildingNumber, address.buildingName, address.subBuildingName,
              address.dependentThoroughfareName, address.thoroughfareName, address.dependentLocality, address.postTown,
              address.postCode))
        }
        uprnAddressPairs.sortBy(kvp => kvp.address) // Sort after translating to drop down format.
      case None =>
        // Handle no results for this postcode.
        log.info(s"No results returned for postcode: ${LogFormats.anonymize(postcode)}")
        Seq.empty
    }
  }

  private def addressRulePicker(poBoxNumber: Option[String] = None, buildingNumber: Option[String] = None, buildingName: Option[String] = None,
                                subBuildingName: Option[String] = None, dependentThoroughfareName: Option[String], thoroughfareName: Option[String] = None,
                                dependentLocality: Option[String] = None, postTown: String, postCode: String): String = {
    val (line1, line2, line3) =
      (poBoxNumber, buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName, dependentLocality) match {
        case (None, None, Some(_), Some(_), None, Some(_), None) => rule8(buildingName, subBuildingName, thoroughfareName)
        case (None, Some(_), None, None, None, Some(_), _) => rule7(buildingNumber, thoroughfareName, dependentLocality)
        case (Some(_), _, _, _, _, _, _) => rule1(poBoxNumber, thoroughfareName, dependentLocality)
        case (_, None, _, None, _, _, None) => rule2(buildingName, dependentThoroughfareName, thoroughfareName)
        case (_, _, None, None, _, _, _) => rule3(buildingNumber, dependentThoroughfareName, thoroughfareName, dependentLocality)
        case (_, None, _, None, _, _, _) => rule4(buildingName, dependentThoroughfareName, thoroughfareName, dependentLocality)
        case (_, Some(_), _, _, _, Some(_), _) => rule6(buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName, dependentLocality)
        case (_, _, _, _, _, _, None) => rule5(buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName)
        case _ => rule6(buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName, dependentLocality)
      }
    line1 + line2 + line3 + postTown + separator + postCode
  }

  //rule methods will build and return three strings for address line1, line2 and line3
  private def rule1(poBoxNumber: Option[String], thoroughfareName: Option[String], dependentLocality: Option[String]): (String, String, String) = {
    (lineBuild(Seq(poBoxNumber)), lineBuild(Seq(thoroughfareName)), lineBuild(Seq(dependentLocality)))
  }

  private def rule2(buildingName: Option[String], dependentThoroughfareName: Option[String], thoroughfareName: Option[String]): (String, String, String) = {
    (lineBuild(Seq(buildingName)), lineBuild(Seq(dependentThoroughfareName)), lineBuild(Seq(thoroughfareName)))
  }

  private def rule3(buildingNumber: Option[String], dependentThoroughfareName: Option[String], thoroughfareName: Option[String], dependentLocality: Option[String])
  : (String, String, String) = {
    (lineBuild(Seq(buildingNumber, dependentThoroughfareName)), lineBuild(Seq(thoroughfareName)), lineBuild(Seq(dependentLocality)))
  }

  private def rule4(buildingName: Option[String], dependentThoroughfareName: Option[String], thoroughfareName: Option[String],
                    dependentLocality: Option[String]): (String, String, String) = {
    (lineBuild(Seq(buildingName, dependentThoroughfareName)), lineBuild(Seq(thoroughfareName)), lineBuild(Seq(dependentLocality)))
  }

  private def rule5(buildingNumber: Option[String], buildingName: Option[String], subBuildingName: Option[String],
                    dependentThoroughfareName: Option[String], thoroughfareName: Option[String]): (String, String, String) = {
    (lineBuild(Seq(subBuildingName, buildingName)), lineBuild(Seq(buildingNumber, dependentThoroughfareName)), lineBuild(Seq(thoroughfareName)))
  }

  private def rule6(buildingNumber: Option[String], buildingName: Option[String], subBuildingName: Option[String],
                    dependentThoroughfareName: Option[String], thoroughfareName: Option[String], dependentLocality: Option[String]): (String, String, String) = {
    (lineBuild(Seq(subBuildingName, buildingName)), lineBuild(Seq(buildingNumber, dependentThoroughfareName, thoroughfareName)), lineBuild(Seq(dependentLocality)))
  }

  private def rule7(buildingNumber: Option[String], thoroughfareName: Option[String], dependentLocality: Option[String]): (String, String, String) = {
    (lineBuild(Seq(buildingNumber, thoroughfareName)), lineBuild(Seq(dependentLocality)), nothing)
  }

  private def rule8(buildingName: Option[String], subBuildingName: Option[String], thoroughfareName: Option[String]): (String, String, String) = {
    (lineBuild(Seq(subBuildingName)), lineBuild(Seq(buildingName)), lineBuild(Seq(thoroughfareName)))
  }

  @tailrec
  private def lineBuild(addressPart: Seq[Option[String]], accumulatedLine: String = nothing): String = {
    if (addressPart.size == 1) lastItemInList(addressPart.head, accumulatedLine, separator)
    else lineBuild(addressPart.tail, accumulateLine(addressPart.head, accumulatedLine, space))
  }

  private def accumulateLine(currentItem: Option[String], accumulatedLine: String, lastChar: String): String = {
    currentItem match {
      case Some(item) => accumulatedLine + item + lastChar
      case _ => accumulatedLine
    }
  }

  private def lastItemInList(lastItem: Option[String], accumulatedLine: String, lastChar: String): String = {
    val currentAddressLine = if (accumulatedLine.takeRight(1) == space) accumulatedLine.dropRight(1) else accumulatedLine
    lastItem match {
      case Some(item) => accumulatedLine + item + lastChar
      case _ => if (currentAddressLine + lastChar == lastChar) nothing
      else currentAddressLine + lastChar
    }
  }

  private def buildAddressViewModel(uprn: Long, resp: Option[Response]): Option[AddressViewModel] = {
    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap {
          _.DPA
        }
        log.info(s"Returning result for uprn request ${LogFormats.anonymize(uprn.toString)}")
        require(addresses.length >= 1, s"Should be at least one address for the UPRN")
        Some(AddressViewModel(
          uprn = Some(addresses.head.UPRN.toLong),
          address = addressRulePicker(addresses.head.poBoxNumber, addresses.head.buildingNumber, addresses.head.buildingName, addresses.head.subBuildingName,
            addresses.head.dependentThoroughfareName, addresses.head.thoroughfareName, addresses.head.dependentLocality,
            addresses.head.postTown, addresses.head.postCode).split(", ")
        )) // Translate to view model.
      case None =>
        log.info(s"No results returned by web service for submitted UPRN: ${LogFormats.anonymize(uprn.toString)}")
        None
    }
  }


  private def checkStatusCodeAndUnmarshal(implicit unmarshaller: FromResponseUnmarshaller[Response]): Future[HttpResponse] => Future[Option[Response]] =
    (futRes: Future[HttpResponse]) => futRes.map {
      res =>
        if (res.status == StatusCodes.OK) Some(unmarshal[Response](unmarshaller)(res))
        else None
    }

  def callPostcodeToAddressOSWebService(request: PostcodeToAddressLookupRequest): Future[Option[Response]] = {
    import spray.httpx.PlayJsonSupport._

    val pipeline: HttpRequest => Future[Option[Response]] = sendReceive ~> checkStatusCodeAndUnmarshal
    val endPoint = postcodeUrlBuilder.endPoint(request)

    pipeline {
      Get(endPoint)
    }

  }

  def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[Response]] = {

    import spray.httpx.PlayJsonSupport._

    val pipeline: HttpRequest => Future[Option[Response]] = sendReceive ~> checkStatusCodeAndUnmarshal
    val endPoint = uprnUrlBuilder.endPoint(request)

    pipeline {
      Get(endPoint)
    }
  }

  override def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = {
    log.info(s"Dealing with the post request for postcode ${LogFormats.anonymize(request.postcode)}")

    if (request.postcode == cannedPostcode) {
      Future {
        cannedPostcodeToAddressResponse
      }
    }
    else {
      callPostcodeToAddressOSWebService(request).map {
        resp => {
          PostcodeToAddressResponse(buildUprnAddressPairSeq(request.postcode, resp))
        }
      }.recover {
        case e: Throwable =>
          log.info(s"Ordnance Survey postcode lookup service error: ${e.toString}")
          PostcodeToAddressResponse(Seq.empty)
      }
    }
  }

  override def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = {
    log.info(s"Dealing with the post request for uprn ${LogFormats.anonymize(request.uprn.toString)}")

    if (request.uprn == cannedUprn) {
      Future {
        cannedUprnToAddressResponse
      }
    }
    else {
      callUprnToAddressOSWebService(request).map {
        resp => {
          UprnToAddressResponse(buildAddressViewModel(request.uprn, resp))
        }
      }.recover {
        case e: Throwable =>
          log.info(s"Ordnance Survey uprn lookup service error: ${e.toString}")
          UprnToAddressResponse(None)
      }
    }
  }
}

object LookupCommand {
  // Must be in a valid format yet not exist.
  private[ordnance_survey_preproduction] final val cannedPostcode = "QQ99QQ"
  private[ordnance_survey_preproduction] final val cannedAddress = "Not real street, Not real town, QQ9 9QQ"
  private[ordnance_survey_preproduction] final val cannedUprn = 999999999999L
  private[ordnance_survey_preproduction] val cannedPostcodeToAddressResponse = PostcodeToAddressResponse(addresses = Seq(UprnAddressPair(cannedUprn.toString, cannedAddress)))
  private[ordnance_survey_preproduction] val cannedUprnToAddressResponse = UprnToAddressResponse(addressViewModel = Some(AddressViewModel(uprn = Some(cannedUprn), address = Seq("Not real street", "Not real town", cannedPostcode))))
}