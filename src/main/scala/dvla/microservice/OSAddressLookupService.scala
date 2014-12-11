package dvla.microservice

import akka.event.LoggingAdapter
import dvla.common.microservice.SprayHttpService
import dvla.domain.address_lookup.{PostcodeToAddressLookupRequest, UprnToAddressLookupRequest}
import spray.http.StatusCodes.ServiceUnavailable
import spray.routing.HttpService
import spray.routing.Route
import dvla.domain.JsonFormats._

import scala.util.{Failure, Success}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
final class SprayOSAddressLookupService(val configuration: Configuration, override val command: AddressLookupCommand)
  extends SprayHttpService with OSAddressLookupService

// this trait defines our service behavior independently from the service actor
trait OSAddressLookupService extends HttpService {

  def log: LoggingAdapter
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
          val showBusinessName: Option[Boolean] = Some(params.get("showBusinessName").exists(_.toBoolean))
          val languageCode: Option[String] = params.get("languageCode")
          languageCode match {
            case Some(_) => lookupPostcode(postcode = postcode, showBusinessName = showBusinessName, languageCode = languageCode) // Language specified so search with filter and if no results then search unfiltered.
            case None => lookupPostcode(postcode, showBusinessName) // No language specified so go straight to unfiltered search.
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

  private def lookupPostcode(postcode: String, showBusinessName: Option[Boolean], languageCode: Option[String]): Route = {
    val request = PostcodeToAddressLookupRequest(postcode = postcode, languageCode = languageCode, showBusinessName = showBusinessName)
    onComplete(command(request)) {
      case Success(resp) if resp.addresses.isEmpty => lookupPostcode(postcode, showBusinessName)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupPostcode(postcode: String, showBusinessName: Option[Boolean]): Route = {
    val request = PostcodeToAddressLookupRequest(postcode = postcode, showBusinessName = showBusinessName)
    onComplete(command(request)) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long, languageCode: Option[String]): Route = {
    val request = UprnToAddressLookupRequest(uprn, languageCode)
    onComplete(command(request)) {
      case Success(resp) if resp.addressViewModel.isEmpty => lookupUprn(uprn)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long): Route = {
    val request = UprnToAddressLookupRequest(uprn)
    onComplete(command(request)) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }
}
