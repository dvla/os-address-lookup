package dvla.microservice

import akka.event.LoggingAdapter
import dvla.common.clientsidesession.TrackingId
import dvla.common.microservice.HttpHeaders.`Tracking-Id`
import dvla.common.microservice.SprayHttpService
import dvla.domain.address_lookup.{PostcodeToAddressLookupRequest, UprnToAddressLookupRequest}
import dvla.domain.JsonFormats._
import spray.http.StatusCodes.ServiceUnavailable
import spray.routing.HttpService
import spray.routing.Route
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
          headerValueByName(`Tracking-Id`.name) {
            trackingId => {
              val postcode: String = params.get("postcode").get
              val languageCode: Option[String] = params.get("languageCode")
              languageCode match {
                case Some(_) =>
                  lookupPostcode(
                    postcode = postcode,
                    languageCode = languageCode
                  )(TrackingId(trackingId)) // Language specified so search with filter and if no results then search unfiltered.
                case None => lookupPostcode(postcode)(TrackingId(trackingId)) // No language specified so go straight to unfiltered search.
              }
            }
          }
        }
      } ~
      pathPrefix("uprn-to-address") {
        parameterMap { params =>
          headerValueByName(`Tracking-Id`.name) {
            trackingId => {
              val uprn: Long = params.get("uprn").get.toLong
              val languageCode: Option[String] = params.get("languageCode")
              languageCode match {
                case Some(_) => lookupUprn(uprn, languageCode)(TrackingId(trackingId))
                case None => lookupUprn(uprn)(TrackingId(trackingId))
              }
            }
          }
        }
      } ~
      pathPrefix("addresses") {
        parameterMap { params =>
          headerValueByName(`Tracking-Id`.name) {
            trackingId => {
              addresses(params.get("postcode").get, params.get("languageCode"))(TrackingId(trackingId))
            }
          }
        }
      }
    }
  }

  private def lookupPostcode(postcode: String, languageCode: Option[String])
                            (implicit trackingId: TrackingId): Route = {
    val request = PostcodeToAddressLookupRequest(
      postcode = postcode,
      languageCode = languageCode
    )
    onComplete(command(request)) {
      case Success(resp) if resp.addresses.isEmpty => lookupPostcode(postcode)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupPostcode(postcode: String)
                            (implicit trackingId: TrackingId): Route = {
    val request = PostcodeToAddressLookupRequest(postcode = postcode)
    onComplete(command(request)) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  // This is the only method that needs to stay in. The rest are legacy
  private def addresses(postcode: String, languageCode: Option[String])(implicit trackingId: TrackingId): Route = {
    val request = PostcodeToAddressLookupRequest(postcode, languageCode)
    val result = command.applyDetailedResult(request)
    onComplete(result) {
      case Success(addressesSeq)
        if addressesSeq.isEmpty && languageCode.isDefined => addresses(postcode, None)
      case Success(resp) =>
        complete(resp)
      case Failure(_) =>
        complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long, languageCode: Option[String])
                        (implicit trackingId: TrackingId): Route = {
    val request = UprnToAddressLookupRequest(uprn, languageCode)
    onComplete(command(request)) {
      case Success(resp) if resp.addressViewModel.isEmpty => lookupUprn(uprn)
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long)
                        (implicit trackingId: TrackingId): Route = {
    val request = UprnToAddressLookupRequest(uprn)
    onComplete(command(request)) {
      case Success(resp) => complete(resp)
      case Failure(_) => complete(ServiceUnavailable)
    }
  }
}
