package dvla.microservice

import spray.testkit.ScalatestRouteTest
import spray.http.HttpRequest
import scala.concurrent.Future
import akka.event.NoLogging
import org.scalatest.WordSpec
import org.scalatest.Matchers

class RouteSpecBase extends WordSpec with ScalatestRouteTest with Matchers with QASAddressLookupService {
  def actorRefFactory = system
  def sendRequest(request: HttpRequest)(implicit timeout: akka.util.Timeout): Future[Any] = ???
  val log = NoLogging
  val baseUrl = "http://localhost/testurl"
  val timeoutInMillisSoap = 0
  override val configuration = Configuration(baseUrl, timeoutInMillisSoap)
  override val command: CannedAddressLookupCommand = new CannedAddressLookupCommand(configuration)
}
