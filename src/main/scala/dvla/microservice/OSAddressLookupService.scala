package dvla.microservice

import akka.event.LoggingAdapter
import dvla.common.LogFormats.DVLALogger
import dvla.common.clientsidesession.TrackingId
import dvla.common.microservice.HttpHeaders.`Tracking-Id`
import dvla.common.microservice.SprayHttpService
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
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

  //NOTE:
  // VM calls addresses => uk.gov.dvla.vehicles.presentation.common.webserviceclients.addresslookup.AddressLookupService#addressesToDropDown
  // PR calls addresses via javascipt (VPC) => uk.gov.dvla.vehicles.presentation.common.controllers.AddressLookup#byPostcode
  val route = {
    get {
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

  private def addresses(postcode: String, languageCode: Option[String])
                       (implicit trackingId: TrackingId, log: LoggingAdapter): Route = {
    logMessage(trackingId, Info, "Received http GET request on os address lookup /addresses")

    val request = PostcodeToAddressLookupRequest(postcode, languageCode)
    val result = command(request)
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

}
