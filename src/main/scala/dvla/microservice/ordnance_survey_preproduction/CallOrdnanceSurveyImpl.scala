package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import dvla.domain.address_lookup.{PostcodeToAddressLookupRequest, UprnToAddressLookupRequest}
import dvla.domain.ordnance_survey_preproduction.Response
import spray.client.pipelining.{Get, sendReceive, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import spray.http.HttpRequest
import spray.httpx.PlayJsonSupport._

class CallOrdnanceSurveyImpl(postcodeUrlBuilder: PostcodeUrlBuilder,
                             uprnUrlBuilder: UprnUrlBuilder)(implicit system: ActorSystem) extends CallOrdnanceSurvey {

  // Postcode to sequence of addresses
  def call(request: PostcodeToAddressLookupRequest): Future[Option[Response]] = {
    val pipeline: HttpRequest => Future[Option[Response]] = sendReceive ~> checkStatusCodeAndUnmarshal
    val endPoint = postcodeUrlBuilder.endPoint(request)
    pipeline {
      Get(endPoint)
    }
  }

  // Uprn to single address
  def call(request: UprnToAddressLookupRequest): Future[Option[Response]] = {
    val pipeline: HttpRequest => Future[Option[Response]] = sendReceive ~> checkStatusCodeAndUnmarshal
    val endPoint = uprnUrlBuilder.endPoint(request)
    pipeline {
      Get(endPoint)
    }
  }
}