package dvla.domain.ordnance_survey_preproduction

import play.api.libs.json.Json

final case class Result(DPA: Option[DPA], LPI: Option[LPI])

object Result {
  implicit val formatResult = Json.reads[Result]
}