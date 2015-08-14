package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup.{PostcodeToAddressLookupRequest, UprnToAddressLookupRequest}
import dvla.domain.ordnance_survey_preproduction.Response
import spray.client.pipelining.unmarshal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import spray.http.{HttpResponse, StatusCodes}
import spray.httpx.unmarshalling.FromResponseUnmarshaller

trait CallOrdnanceSurvey {

  type ConvertToOsResponse = Future[HttpResponse] => Future[Option[Response]]
  type ResponseUnmarshaller = FromResponseUnmarshaller[Response]

  def call(request: PostcodeToAddressLookupRequest): Future[Option[Response]]

  def call(request: UprnToAddressLookupRequest): Future[Option[Response]]

  protected def checkStatusCodeAndUnmarshal(implicit unmarshaller: ResponseUnmarshaller): ConvertToOsResponse =
    (futRes: Future[HttpResponse]) => futRes.map { res =>
      if (res.status == StatusCodes.OK) Some(unmarshal[Response](unmarshaller)(res))
      else None
    }
}
