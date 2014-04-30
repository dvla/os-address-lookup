package dvla.microservice

import spray.routing.HttpService
import scala.concurrent.Future
import spray.http.StatusCodes._
import dvla.domain.address_lookup._
import scala.util.{Failure, Success}
import akka.event.LoggingAdapter
import dvla.domain.JsonFormats._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class SprayQASAddressLookupService(val configuration: Configuration)(implicit val command: AddressLookupCommand) extends SprayHttpService with QASAddressLookupService

// this trait defines our service behavior independently from the service actor
trait QASAddressLookupService extends HttpService {

  val log: LoggingAdapter
  val configuration: Configuration
  val command: AddressLookupCommand

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  // TODO Work out if we want to dispatch soap requests on a different dispatcher
  private implicit def executionContext = actorRefFactory.dispatcher

  // TODO this should really be a GET not a POST
  val route = {
    post {
      pathPrefix("postcode-to-address") {
          entity(as[AddressLookupRequest]) { postcodeToAddressResponse =>
            onComplete(lookupAddress(postcodeToAddressResponse)) {
              case Success(resp) => complete(resp)
              case Failure(_) => complete(ServiceUnavailable)
            }
          }
      }
    }
  }

  private def lookupAddress(request: AddressLookupRequest): Future[PostcodeToAddressResponse] = {
    log.debug(s"Received post request on postcode-to-address. Request object = ${request}")
    command(request)
  }

}