package dvla.microservice

import spray.routing.HttpService
import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes._
import scala.util.{Failure, Success}
import akka.event.LoggingAdapter
import dvla.domain.JsonFormats._
import dvla.domain.address_lookup._
import dvla.common.microservice.SprayHttpService
import dvla.domain.LogFormats

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
final class SprayOSAddressLookupService(val configuration: Configuration)(implicit val command: AddressLookupCommand) extends SprayHttpService with OSAddressLookupService

// this trait defines our service behavior independently from the service actor
trait OSAddressLookupService extends HttpService {

  val log: LoggingAdapter
  val configuration: Configuration
  val command: AddressLookupCommand

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  // TODO Work out if we want to dispatch requests on ordnance_survey different dispatcher
  private implicit def executionContext = actorRefFactory.dispatcher

  val route = {
    get {
      pathPrefix("postcode-to-address") {
        parameterMap { params =>
          val postcode: String = params.get("postcode").get
          val languageCode: Option[String] = params.get("languageCode")
          languageCode match {
            case Some(l) => lookupWithLanguageFilter(postcode = postcode, languageCode = languageCode) // Language specified so search with filter and if no results then search unfiltered.
            case None => lookupWithNoLanguageFilter(postcode) // No language specified so go straight to unfiltered search.
          }
        }
      } ~
      pathPrefix("uprn-to-address") {
        parameterMap { params =>
          onComplete(lookupAddress(UprnToAddressLookupRequest(params.get("uprn").get.toLong, params.get("languageCode")))) {
            case Success(resp) => complete(resp)
            case Failure(_) => complete(ServiceUnavailable)
          }
        }
      }
    }
  }

  private def lookupWithLanguageFilter(postcode: String, languageCode: Option[String]) =
    onComplete(lookupAddress(PostcodeToAddressLookupRequest(postcode, languageCode))) {
      case Success(resp) if resp.addresses.isEmpty => lookupWithNoLanguageFilter(postcode)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }

  private def lookupWithNoLanguageFilter(postcode: String) =
    onComplete(lookupAddress(PostcodeToAddressLookupRequest(postcode))) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }

  private def lookupAddress(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = {
    log.debug(s"Received post request on postcode-to-address ${LogFormats.anonymize(request.postcode)}") //Request object = ${request}")
    command(request)
  }

  private def lookupAddress(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = {
    log.debug(s"Received post request on uprn-to-address ${LogFormats.anonymize(request.uprn.toString)}") //. Request object = ${request}")
    command(request)
  }

}