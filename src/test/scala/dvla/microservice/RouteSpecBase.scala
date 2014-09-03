package dvla.microservice

import akka.event.NoLogging
import dvla.microservice.ordnance_survey_preproduction.LookupCommand
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import spray.testkit.ScalatestRouteTest

class RouteSpecBase extends WordSpec with ScalatestRouteTest with Matchers with OSAddressLookupService with MockitoSugar {

  val log = NoLogging
  final val osUsername = "username"
  final val osPassword = "password"
  final val osBaseUrl = "http://localhost/testurl"
  final val osRequestTimeout = 0
  override val configuration = Configuration(osUsername, osPassword, osBaseUrl)
  override val command = mock[LookupCommand]

  def actorRefFactory = system
}
