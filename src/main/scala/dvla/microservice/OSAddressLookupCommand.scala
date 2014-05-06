package dvla.microservice

import scala.util.Failure
import akka.actor.ActorSystem
import scala.concurrent._
import akka.event.Logging
import dvla.domain.address_lookup._
import spray.client.pipelining._

import spray.http.{HttpResponse, StatusCodes, HttpRequest, BasicHttpCredentials}
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import scala.Some
import dvla.domain.ordnance_survey.{OSAddressbaseSearchResponse, OSAddressbaseDPA}
import dvla.domain.address_lookup.AddressViewModel
import spray.httpx.unmarshalling.FromResponseUnmarshaller

class OSAddressLookupCommand(val configuration: Configuration)(implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {

  val username = s"${configuration.ordnanceSurveyUsername}"
  val password = s"${configuration.ordnanceSurveyPassword}"
  val baseUrl = s"${configuration.ordnanceSurveyBaseUrl}"
  val requestTimeout = configuration.ordnanceSurveyRequestTimeout.toInt

  final lazy val log = Logging(system, this.getClass)

  def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')

  def sort(addresses: Seq[OSAddressbaseDPA]) = {

    addresses.sortBy(addressDpa => {
      val buildingNumber = addressDpa.buildingNumber.getOrElse("0")
      val buildingNumberSanitised = buildingNumber.replaceAll("[^0-9]", "") // Sanitise building number as it could contain letters which would cause toInt to throw e.g. 107a.
      (buildingNumberSanitised, addressDpa.buildingName) // TODO check with BAs how they would want to sort the list
    })
  }

  def buildUprnAddressPairSeq(postcode: String, resp: Option[OSAddressbaseSearchResponse]): Seq[UprnAddressPair] = {

    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap {
          _.DPA
        }
        sort(addresses) map {
          address => UprnAddressPair(address.UPRN, address.address)
        } // Sort before translating to drop down format.
      case None =>
        // Handle no results
        log.debug(s"No results returned for postcode: $postcode")
        Seq.empty
    }

  }

  def buildAddressViewModel(uprn: Long, resp: Option[OSAddressbaseSearchResponse]): Option[AddressViewModel] = {

    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap {
          _.DPA
        }
        require(addresses.length >= 1, s"Should be at least one address for the UPRN")
        Some(AddressViewModel(uprn = Some(addresses.head.UPRN.toLong), address = addresses.head.address.split(", "))) // Translate to view model.
      case None =>
        log.error(s"No results returned by web service for submitted UPRN: $uprn")
        None
    }

  }

  def checkStatusCodeAndUnmarshal(implicit unmarshaller: FromResponseUnmarshaller[OSAddressbaseSearchResponse]): Future[HttpResponse] => Future[Option[OSAddressbaseSearchResponse]] =
    (futRes: Future[HttpResponse]) => futRes.map {
      res =>
        if (res.status == StatusCodes.OK) Some(unmarshal[OSAddressbaseSearchResponse](unmarshaller)(res))
        else None
    }

  def callPostcodeToAddressOSWebService(request: PostcodeToAddressLookupRequest): Future[Option[OSAddressbaseSearchResponse]] = {

    import spray.httpx.PlayJsonSupport._

    val pipeline: HttpRequest => Future[Option[OSAddressbaseSearchResponse]] = (
      addCredentials(BasicHttpCredentials(username, password))
        ~> (sendReceive
        ~> checkStatusCodeAndUnmarshal)
      )

    val endPoint = s"$baseUrl/postcode?postcode=${postcodeWithNoSpaces(request.postcode)}&dataset=dpa"

    pipeline {
      Get(endPoint)
    }

  }

  def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[OSAddressbaseSearchResponse]] = {

    import spray.httpx.PlayJsonSupport._

    val pipeline: HttpRequest => Future[Option[OSAddressbaseSearchResponse]] = (
      addCredentials(BasicHttpCredentials(username, password))
        ~> (sendReceive
        ~> checkStatusCodeAndUnmarshal)
      )

    val endPoint = s"$baseUrl/uprn?uprn=${request.uprn}&dataset=dpa" // TODO add lpi to URL, but need to set orgnaisation as Option on the type.

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

