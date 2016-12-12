package dvla.microservice.ordnance_survey_preproduction

import akka.actor.ActorSystem
import akka.event.Logging
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.domain.ordnance_survey_preproduction.Response
import spray.client.pipelining.{Get, sendReceive, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import spray.http.HttpRequest
import spray.httpx.PlayJsonSupport.playJsonUnmarshaller

class CallOrdnanceSurveyImpl(postcodeUrlBuilder: PostcodeUrlBuilder)
                            (implicit system: ActorSystem) extends CallOrdnanceSurvey {

  private implicit val log = Logging(system, this.getClass)

  // Postcode to sequence of addresses
  def call(request: PostcodeToAddressLookupRequest)(implicit trackingId: TrackingId): Future[Option[Response]] = {
    val pipeline: HttpRequest => Future[Option[Response]] = sendReceive ~> checkStatusCodeAndUnmarshal
    val endPoint = postcodeUrlBuilder.endPoint(request)
    pipeline {
      Get(endPoint)
    }
  }

}