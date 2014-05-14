package dvla.domain.ordnance_survey_beta_0_6

import play.api.libs.json.Json

case class Result(DPA: Option[DPA],
                               LPI: Option[LPI])

object Result {
  implicit val formatResult = Json.format[Result]
}