package dvla.microservice.ordnance_survey_preproduction

import akka.event.LoggingAdapter
import dvla.common.LogFormats.DVLALogger
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup.{PostcodeToAddressLookupRequest, UprnToAddressLookupRequest}
import dvla.domain.ordnance_survey_preproduction.Response
import spray.client.pipelining.unmarshal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import spray.http.{HttpResponse, StatusCodes}
import spray.httpx.unmarshalling.FromResponseUnmarshaller

trait CallOrdnanceSurvey extends DVLALogger {

  type ConvertToOsResponse = Future[HttpResponse] => Future[Option[Response]]
  type ResponseUnmarshaller = FromResponseUnmarshaller[Response]

  def call(request: PostcodeToAddressLookupRequest)(implicit trackingId: TrackingId): Future[Option[Response]]

  def call(request: UprnToAddressLookupRequest)(implicit trackingId: TrackingId): Future[Option[Response]]

  protected def checkStatusCodeAndUnmarshal(implicit unmarshaller: ResponseUnmarshaller,
                                            trackingId: TrackingId,
                                            loggingAdapter: LoggingAdapter): ConvertToOsResponse =
    (futRes: Future[HttpResponse]) => futRes.map { res =>
      if (res.status == StatusCodes.OK) {
        val msg = s"Received the following status when calling Ordnance Survey: ${res.status}"
        logMessage(trackingId, Info, msg)
        Some(unmarshal[Response](unmarshaller)(res))
      }
      else {
        val emptyString = ""
        val msg = s"Received the following status when calling Ordnance Survey: ${res.status} " +
          s"in response: ${res.entity.toString.replaceAll("\\n|\\r/g", emptyString)}"
        logMessage(trackingId, Error, msg)
        None
      }
    }
}
