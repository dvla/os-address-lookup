package dvla.microservice.ordnance_survey_beta_0_6

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.LoggingAdapter
import dvla.common.LogFormats
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.UprnToAddressLookupRequest
import dvla.domain.address_lookup.UprnToAddressResponse
import dvla.domain.ordnance_survey_beta_0_6.DPA
import dvla.domain.ordnance_survey_beta_0_6.Response
import dvla.microservice.AddressLookupCommand
import dvla.microservice.Configuration
import spray.client.pipelining._
import spray.http.BasicHttpCredentials
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.httpx.unmarshalling.FromResponseUnmarshaller

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class LookupCommand(val configuration: Configuration)
                   (implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {

  private val username = configuration.username
  private val password = configuration.password
  private val baseUrl = configuration.baseUrl
  private lazy val log: LoggingAdapter = Logging(system, this.getClass)

  private def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')

  private def sort(addresses: Seq[DPA]) = {
    addresses.sortBy(addressDpa => {
      val buildingNumber = addressDpa.buildingNumber.getOrElse("0")
      val buildingNumberSanitised = buildingNumber.replaceAll("[^0-9]", "") // Sanitise building number as it could contain letters which would cause toInt to throw e.g. 107a.
      (buildingNumberSanitised, addressDpa.buildingName)
    })
  }

  private def buildUprnAddressPairSeq(postcode: String, resp: Option[Response]): Seq[UprnAddressPair] = {
    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap {
          _.DPA
        }
        log.info(s"Returning result for postcode request ${LogFormats.anonymize(postcode)}")
        sort(addresses) map {
          address => UprnAddressPair(address.UPRN, address.address)
        } // Sort before translating to drop down format.
      case None =>
        // Handle no results
        log.debug(s"No results returned for postcode: ${LogFormats.anonymize(postcode)}")
        Seq.empty
    }
  }

  private def buildAddressViewModel(uprn: Long, resp: Option[Response]): Option[AddressViewModel] = {
    val flattenMapResponse = resp.flatMap(_.results)

    flattenMapResponse match {
      case Some(results) =>
        val addresses = results.flatMap (_.DPA)
        log.info(s"Returning result for uprn request ${LogFormats.anonymize(uprn.toString)}")
        require(addresses.length >= 1, s"Should be at least one address for the UPRN")
        Some(AddressViewModel(uprn = Some(addresses.head.UPRN.toLong), address = addresses.head.address.split(", "))) // Translate to view model.
      case None =>
        log.error(s"No results returned by web service for submitted UPRN: ${LogFormats.anonymize(uprn.toString)}")
        None
    }
  }

  type ConvertToOsResponse = Future[HttpResponse] => Future[Option[Response]]
  type ResponseMarshaller = FromResponseUnmarshaller[Response]

  private def checkStatusCodeAndUnmarshal(implicit unmarshaller: ResponseMarshaller): ConvertToOsResponse =
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

    val endPoint = s"$baseUrl/uprn?uprn=${request.uprn}&dataset=dpa"

    pipeline {
      Get(endPoint)
    }
  }

  override def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = {
    log.info(s"Received and handling the request for postcode ${request.postcode}")

    callPostcodeToAddressOSWebService(request).map { resp =>
        PostcodeToAddressResponse(buildUprnAddressPairSeq(request.postcode, resp))
    }.recover {
      case e: Throwable =>
        log.info(s"Ordnance Survey postcode lookup service error: ${e.toString}")
        PostcodeToAddressResponse(Seq.empty)
    }
  }

  override def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = {
    log.info(s"Received and handling the request for uprn ${request.uprn}")

    callUprnToAddressOSWebService(request).map { resp =>
        UprnToAddressResponse(buildAddressViewModel(request.uprn, resp))
    }.recover {
      case e: Throwable =>
        log.info(s"Ordnance Survey uprn lookup service error: ${e.toString}")
        UprnToAddressResponse(None)
    }
  }
}
