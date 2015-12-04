package dvla.microservice

import akka.event.LoggingAdapter
import dvla.common.LogFormats.DVLALogger
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
trait OSAddressLookupService extends HttpService with DVLALogger {

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
                  val msg = "Received http GET request on os address lookup /postcode-to-address with postcode and language code"
                  logMessage(TrackingId(trackingId), Info, msg)(log)
                  lookupPostcode(
                    postcode = postcode,
                    languageCode = languageCode
                  )(TrackingId(trackingId), log) // Language specified so search with filter and if no results then search unfiltered.
                case None =>
                  val msg = "Received http GET request on os address lookup /postcode-to-address with postcode and no language code"
                  logMessage(TrackingId(trackingId), Info, msg)(log)
                  lookupPostcode(postcode)(TrackingId(trackingId), log) // No language specified so go straight to unfiltered search.
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
                case Some(_) =>
                  val msg = "Received http GET request on os address lookup /uprn-to-address with uprn and language code"
                  logMessage(TrackingId(trackingId), Info, msg)(log)
                  lookupUprn(uprn, languageCode)(TrackingId(trackingId), log)
                case None =>
                  val msg = "Received http GET request on os address lookup /uprn-to-address with uprn and no language code"
                  logMessage(TrackingId(trackingId), Info, msg)(log)
                  lookupUprn(uprn)(TrackingId(trackingId), log)
              }
            }
          }
        }
      } ~
      pathPrefix("addresses") {
        parameterMap { params =>
          headerValueByName(`Tracking-Id`.name) {
            trackingId => {
              addresses(params.get("postcode").get, params.get("languageCode"))(TrackingId(trackingId), log)
            }
          }
        }
      }
    }
  }

  private def lookupPostcode(postcode: String, languageCode: Option[String])
                            (implicit trackingId: TrackingId, log: LoggingAdapter): Route = {
    logMessage(trackingId, Info, "Looking up addresses with postcode and language code")
    val request = PostcodeToAddressLookupRequest(
      postcode = postcode,
      languageCode = languageCode
    )
    onComplete(command(request)) {
      case Success(resp) if resp.addresses.isEmpty =>
        logMessage(trackingId, Info, "Found no addresses so now going to lookup again with only the postcode")
        lookupPostcode(postcode)
      case Success(resp) =>
        logMessage(trackingId, Info, s"Request completed successfully")
        complete(resp)
      case Failure(_) =>
        logMessage(trackingId, Error, s"Returning code $ServiceUnavailable for request")
        complete(ServiceUnavailable)
    }
  }

  private def lookupPostcode(postcode: String)
                            (implicit trackingId: TrackingId, log: LoggingAdapter): Route = {
    logMessage(trackingId, Info, "Looking up addresses with postcode only and no language code")
    val request = PostcodeToAddressLookupRequest(postcode = postcode)
    onComplete(command(request)) {
      case Success(resp) =>
        logMessage(trackingId, Info, s"Request completed successfully")
        complete(resp)
      case Failure(_) =>
        logMessage(trackingId, Error, s"Returning code $ServiceUnavailable for request")
        complete(ServiceUnavailable)
    }
  }

  // This is the only method that needs to stay in. The rest are legacy
  private def addresses(postcode: String, languageCode: Option[String])
                       (implicit trackingId: TrackingId, log: LoggingAdapter): Route = {
    logMessage(trackingId, Info, "Received http GET request on os address lookup /addresses")

    val request = PostcodeToAddressLookupRequest(postcode, languageCode)
    val result = command.applyDetailedResult(request)
    onComplete(result) {
      case Success(addressesSeq)
        if addressesSeq.isEmpty && languageCode.isDefined =>
          logMessage(trackingId, Info, "Found no addresses so now going to lookup again with only the postcode")
          addresses(postcode, None)
      case Success(resp) =>
        logMessage(trackingId, Info, s"Request completed successfully")
        complete(resp)
      case Failure(_) =>
        logMessage(trackingId, Error, s"Returning code $ServiceUnavailable for request")
        complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long, languageCode: Option[String])
                        (implicit trackingId: TrackingId, log: LoggingAdapter): Route = {
    logMessage(trackingId, Info, "Looking up addresses with uprn and language code")
    val request = UprnToAddressLookupRequest(uprn, languageCode)
    onComplete(command(request)) {
      case Success(resp) if resp.addressViewModel.isEmpty =>
        logMessage(trackingId, Info, "Found no addresses so now going to lookup again with only the uprn")
        lookupUprn(uprn)
      case Success(resp) =>
        logMessage(trackingId, Info, s"Request completed successfully")
        complete(resp)
      case Failure(_) =>
        logMessage(trackingId, Error, s"Returning code $ServiceUnavailable for request")
        complete(ServiceUnavailable)
    }
  }

  private def lookupUprn(uprn: Long)
                        (implicit trackingId: TrackingId, log: LoggingAdapter): Route = {
    logMessage(trackingId, Info, "Looking up addresses with uprn only and no language code")
    val request = UprnToAddressLookupRequest(uprn)
    onComplete(command(request)) {
      case Success(resp) =>
        logMessage(trackingId, Info, s"Request completed successfully")
        complete(resp)
      case Failure(_) =>
        logMessage(trackingId, Error, s"Returning code $ServiceUnavailable for request")
        complete(ServiceUnavailable)
    }
  }
}
