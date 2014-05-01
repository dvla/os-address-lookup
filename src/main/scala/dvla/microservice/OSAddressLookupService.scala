package dvla.microservice

import spray.routing.HttpService
import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes._
import scala.util.{Failure, Success}
import akka.event.LoggingAdapter
import dvla.domain.JsonFormats._
import dvla.domain.address_lookup._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class SprayOSAddressLookupService(val configuration: Configuration)(implicit val command: AddressLookupCommand) extends SprayHttpService with OSAddressLookupService

// this trait defines our service behavior independently from the service actor
trait OSAddressLookupService extends HttpService {

  val log: LoggingAdapter
  val configuration: Configuration
  val command: AddressLookupCommand

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  // TODO Work out if we want to dispatch soap requests on a different dispatcher
  private implicit def executionContext = actorRefFactory.dispatcher

  // TODO these should really be a GET not a POST
  val route = {
    post {
      pathPrefix("postcode-to-address") {
          entity(as[PostcodeToAddressLookupRequest]) { postcodeToAddressResponse =>
            onComplete(lookupAddress(postcodeToAddressResponse)) {
              case Success(resp) => complete(resp)
              case Failure(_) => complete(ServiceUnavailable)
            }
          }
      } ~
        pathPrefix("uprn-to-address") {
          entity(as[UprnToAddressLookupRequest]) { uprnToAddressResponse =>
            onComplete(lookupAddress(uprnToAddressResponse)) {
              case Success(resp) => complete(resp)
              case Failure(_) => complete(ServiceUnavailable)
            }
          }
        }
    }
  }

  private def lookupAddress(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse] = {
    log.debug(s"Received post request on postcode-to-address. Request object = ${request}")
    command(request)
  }

  private def lookupAddress(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse] = {
    log.debug(s"Received post request on uprn-to-address. Request object = ${request}")
    command(request)
  }

}