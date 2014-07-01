package dvla.microservice

import spray.routing.{Route, HttpService}
import scala.concurrent.Future
import spray.http.StatusCodes._
import scala.util.{Failure, Success}
import akka.event.LoggingAdapter
import dvla.domain.JsonFormats._
import dvla.domain.address_lookup._
import dvla.common.microservice.SprayHttpService
import dvla.common.LogFormats

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
final class SprayOSAddressLookupService(val configuration: Configuration, override val command: AddressLookupCommand)
  extends SprayHttpService with OSAddressLookupService

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
            case Some(_) => lookupPostcode(postcode = postcode, languageCode = languageCode) // Language specified so search with filter and if no results then search unfiltered.
            case None => lookupPostcode(postcode) // No language specified so go straight to unfiltered search.
          }
        }
      } ~
      pathPrefix("uprn-to-address") {
        parameterMap { params =>
          val uprn: Long = params.get("uprn").get.toLong
          val languageCode: Option[String] = params.get("languageCode")
          languageCode match {
            case Some(_) => lookupUprn(uprn, languageCode)
            case None => lookupUprn(uprn)
          }
        }
      }
    }
  }

  private def lookupPostcode(postcode: String, languageCode: Option[String]): Route = {
    val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = languageCode)
    onComplete(command(request)) {
      case Success(resp) if resp.addresses.isEmpty => lookupPostcode(postcode)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupPostcode(postcode: String): Route = {
    val request = PostcodeToAddressLookupRequest(postcode = postcode)
    onComplete(command(request)) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long, languageCode: Option[String]): Route = {
    val request: UprnToAddressLookupRequest = UprnToAddressLookupRequest(uprn, languageCode)
    onComplete(command(request)) {
      case Success(resp) if resp.addressViewModel.isEmpty => lookupUprn(uprn)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long): Route = {
    val request: UprnToAddressLookupRequest = UprnToAddressLookupRequest(uprn)
    onComplete(command(request)) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }
}