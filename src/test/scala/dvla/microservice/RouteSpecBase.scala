package dvla.microservice

import spray.testkit.ScalatestRouteTest
import spray.http.HttpRequest
import scala.concurrent.Future
import akka.event.NoLogging
import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import dvla.microservice.ordnance_survey_beta_0_6.LookupCommand

class RouteSpecBase extends WordSpec with ScalatestRouteTest with Matchers with OSAddressLookupService with MockitoSugar {

  def actorRefFactory = system
  val log = NoLogging
  final val osUsername = "username"
  final val osPassword = "password"
  final val osBaseUrl = "http://localhost/testurl"
  final val osRequestTimeout = 0
  override val configuration = Configuration(osUsername, osPassword, osBaseUrl)
  override val command = mock[LookupCommand]

}
