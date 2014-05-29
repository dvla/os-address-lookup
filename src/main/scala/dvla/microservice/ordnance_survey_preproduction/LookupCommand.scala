package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import scala.concurrent._
import akka.event.Logging
import dvla.domain.address_lookup._
import spray.client.pipelining._
import spray.http.{HttpResponse, StatusCodes, HttpRequest}
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import scala.Some
import dvla.domain.ordnance_survey_preproduction.{Response, DPA}
import dvla.domain.address_lookup.AddressViewModel
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import dvla.microservice.{AddressLookupCommand, Configuration}

class LookupCommand(val configuration: Configuration)(implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {

  private val apiKey = configuration.apiKey
  private val baseUrl = configuration.baseUrl
  private val separator = ", "
  private val nothing = ""
  private val space = " "

  private final lazy val log = Logging(system, this.getClass)

  private def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')

  private def sort(addresses: Seq[DPA]) = {
    addresses.sortBy(addressDpa => {
      val buildingNumber = addressDpa.buildingNumber.getOrElse("0")
      val buildingNumberSanitised = buildingNumber.replaceAll("[^0-9]", "").toInt
       // Sanitise building number as it could contain letters which would cause toInt to throw e.g. 107a.
      (buildingNumberSanitised, addressDpa.address) // TODO check with BAs how they would want to sort the list
    })
  }

  private def buildUprnAddressPairSeq(postcode: String, resp: Option[Response]): Seq[UprnAddressPair] = {
    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap {
          _.DPA
        }
        sort(addresses) map {
          address => UprnAddressPair(address.UPRN,
                                     addressRulePicker(address.poBoxNumber, address.buildingNumber, address.buildingName, address.subBuildingName,
                                                       address.dependentThoroughfareName, address.thoroughfareName, address.dependentLocality, address.postTown,
                                                       address.postCode))
        }
        // Sort before translating to drop down format.
      case None =>
        // Handle no results
        log.debug(s"No results returned for postcode: $postcode")
        Seq.empty
    }
  }

  private def addressRulePicker(poBoxNumber: Option[String] = None, buildingNumber: Option[String] = None, buildingName: Option[String] = None,
                             subBuildingName: Option[String] = None, dependentThoroughfareName: Option[String], thoroughfareName: Option[String] = None,
                             dependentLocality: Option[String] = None, postTown: String, postCode: String):String = {
    //println(s"PO Box Number $poBoxNumber, Building number $buildingNumber, Building name $buildingName, Sub building name $subBuildingName, Dependant thoroughfare name $dependentThoroughfareName, Thoroughfare name $thoroughfareName, Dependent locality $dependentLocality, Post town $postTown, Postcode $postCode")

    val(line1, line2, line3) =
      (poBoxNumber, buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName, dependentLocality) match {
        case (None,None,Some(_),Some(_),None,Some(_),None) => rule8(buildingName, subBuildingName, thoroughfareName)
        case (None,Some(_),None,None,None,Some(_),Some(_)) => rule7(buildingNumber,thoroughfareName,dependentLocality)
        case (Some(_),_,_,_,_,_,_) => rule1(poBoxNumber, thoroughfareName, dependentLocality)
        case (_,None,_,None,_,_,None) => rule2(buildingName, dependentThoroughfareName, thoroughfareName)
        case (_,_,None,None,_,_,_) => rule3(buildingNumber, dependentThoroughfareName,thoroughfareName, dependentLocality)
        case (_,None,_,None,_,_,_) => rule4(buildingName, dependentThoroughfareName, thoroughfareName, dependentLocality)
        case (_,Some(_),_,_,_,Some(_),_) => rule6(buildingNumber, buildingName, subBuildingName, dependentThoroughfareName,thoroughfareName, dependentLocality)
        case (_,_,_,_,_,_,None) => rule5(buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName)
        case _ => rule6(buildingNumber, buildingName, subBuildingName, dependentThoroughfareName, thoroughfareName, dependentLocality)
      }
    line1 + line2 + line3 + postTown + separator + postCode
  }

  //rule methods will build and return three strings for address line1, line2 and line3
  private def rule1(poBoxNumber: Option[String], thoroughfareName : Option[String], dependentLocality : Option[String]) : (String, String, String)  = {
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

  private def rule7(buildingNumber: Option[String],thoroughfareName: Option[String],dependentLocality: Option[String]): (String, String, String) = {
    (lineBuild(Seq(buildingNumber,thoroughfareName)), lineBuild(Seq(dependentLocality)), nothing)
  }

  private def rule8(buildingName: Option[String], subBuildingName : Option[String],thoroughfareName: Option[String]): (String, String, String) = {
    (lineBuild(Seq(subBuildingName)), lineBuild(Seq(buildingName)), lineBuild(Seq(thoroughfareName)))
  }

  private def lineBuild (addressPart: Seq[Option[String]], accumulatedLine: String = nothing): String = {
    if (addressPart.size == 1) lastItemInList(addressPart.head, accumulatedLine, separator)
    else lineBuild(addressPart.tail, accumulateLine(addressPart.head, accumulatedLine, space))
  }

  private def accumulateLine(currentItem: Option[String], accumulatedLine: String, lastChar: String):String = {
    currentItem match {
      case Some(currentItem) => accumulatedLine + currentItem + lastChar
      case _ => accumulatedLine
    }
  }

  private def lastItemInList(lastItem: Option[String], accumulatedLine: String, lastChar: String): String = {
    val currentAddressLine = if (accumulatedLine.takeRight(1) == space) accumulatedLine.dropRight(1) else accumulatedLine
    lastItem match {
      case Some(lastItem) => accumulatedLine + lastItem + lastChar
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
        require(addresses.length >= 1, s"Should be at least one address for the UPRN")
        Some(AddressViewModel(
          uprn = Some(addresses.head.UPRN.toLong),
          address = addressRulePicker(addresses.head.poBoxNumber, addresses.head.buildingNumber, addresses.head.buildingName, addresses.head.subBuildingName,
            addresses.head.dependentThoroughfareName, addresses.head.thoroughfareName, addresses.head.dependentLocality,
            addresses.head.postTown, addresses.head.postCode).split(", ")
        )) // Translate to view model.
      case None =>
        log.error(s"No results returned by web service for submitted UPRN: $uprn")
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

    val endPoint = s"$baseUrl/postcode?postcode=${postcodeWithNoSpaces(request.postcode)}&dataset=dpa&key=$apiKey"

    pipeline {
      Get(endPoint)
    }

  }

  def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[Response]] = {

    import spray.httpx.PlayJsonSupport._

    val pipeline: HttpRequest => Future[Option[Response]] = sendReceive ~> checkStatusCodeAndUnmarshal

    val endPoint = s"$baseUrl/uprn?uprn=${request.uprn}&dataset=dpa&key=$apiKey"

    pipeline {
      Get(endPoint)
    }

  }

  override def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = {

    log.debug("Dealing with the post request on postcode-to-address with OS data response...")
    log.debug("... for postcode " + request.postcode)

    callPostcodeToAddressOSWebService(request).map {
      resp => {
        PostcodeToAddressResponse(buildUprnAddressPairSeq(request.postcode, resp))
      }
    }.recover {
      case e: Throwable =>
        log.error(s"Ordnance Survey postcode lookup service error: $e")
        PostcodeToAddressResponse(Seq.empty)
    }

  }

  override def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = {

    log.debug("Dealing with the post request on uprn-to-address with OS data response...")
    log.debug("... for uprn " + request.uprn)

    callUprnToAddressOSWebService(request).map {
      resp => {
        UprnToAddressResponse(buildAddressViewModel(request.uprn, resp))
      }
    }.recover {
      case e: Throwable =>
        log.error(s"Ordnance Survey uprn lookup service error: $e")
        UprnToAddressResponse(None)
    }
  }
}

