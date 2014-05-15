package dvla.microservice.ordnance_survey_beta_0_6

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
import dvla.domain.ordnance_survey_beta_0_6.{Response, DPA}
import dvla.domain.address_lookup.AddressViewModel
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import dvla.microservice.{AddressLookupCommand, Configuration}

class LookupCommand(val configuration: Configuration)(implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {

  private val username = s"${configuration.ordnanceSurveyUsername}"
  private val password = s"${configuration.ordnanceSurveyPassword}"
  private val baseUrl = s"${configuration.ordnanceSurveyBaseUrl}"
  private val requestTimeout = configuration.ordnanceSurveyRequestTimeout.toInt

  private final lazy val log = Logging(system, this.getClass)

  private def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')

  private def sort(addresses: Seq[DPA]) = {
    addresses.sortBy(addressDpa => {
      val buildingNumber = addressDpa.buildingNumber.getOrElse("0")
      val buildingNumberSanitised = buildingNumber.replaceAll("[^0-9]", "") // Sanitise building number as it could contain letters which would cause toInt to throw e.g. 107a.
      (buildingNumberSanitised, addressDpa.buildingName) // TODO check with BAs how they would want to sort the list
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
          address => UprnAddressPair(address.UPRN, address.address)
        } // Sort before translating to drop down format.
      case None =>
        // Handle no results
        log.debug(s"No results returned for postcode: $postcode")
        Seq.empty
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
        Some(AddressViewModel(uprn = Some(addresses.head.UPRN.toLong), address = addresses.head.address.split(", "))) // Translate to view model.
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

    val pipeline: HttpRequest => Future[Option[Response]] = (
      addCredentials(BasicHttpCredentials(username, password))
        ~> (sendReceive
        ~> checkStatusCodeAndUnmarshal)
      )

    val endPoint = s"$baseUrl/postcode?postcode=${postcodeWithNoSpaces(request.postcode)}&dataset=dpa"

    pipeline {
      Get(endPoint)
    }

  }

  def callUprnToAddressOSWebService(request: UprnToAddressLookupRequest): Future[Option[Response]] = {

    import spray.httpx.PlayJsonSupport._

    val pipeline: HttpRequest => Future[Option[Response]] = (
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

