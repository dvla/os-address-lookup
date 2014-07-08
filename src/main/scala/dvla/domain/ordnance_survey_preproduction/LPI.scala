package dvla.domain.ordnance_survey_preproduction

import play.api.libs.json.Json

final case class LPI(UPRN: String,
                     ADDRESS: String)

object LPI {
  implicit val formatLPI = Json.reads[LPI]
}