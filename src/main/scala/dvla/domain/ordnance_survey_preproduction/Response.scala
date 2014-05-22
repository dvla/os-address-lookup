package dvla.domain.ordnance_survey_preproduction

import play.api.libs.json.Json

case class Response(header: Header,
                    results: Option[Seq[Result]])

object Response {
  implicit val formatResponse = Json.reads[Response]
}