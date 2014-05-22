package dvla.domain.ordnance_survey_beta_0_6

import play.api.libs.json.Json

final case class Response(header: Header,
                    results: Option[Seq[Result]])

object Response {
  implicit val formatResponse = Json.format[Response]
}