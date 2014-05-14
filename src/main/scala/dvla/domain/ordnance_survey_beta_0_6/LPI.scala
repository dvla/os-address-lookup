package dvla.domain.ordnance_survey_beta_0_6

import play.api.libs.json.Json

case class LPI(UPRN: String,
                            ADDRESS: String,
                            USRN: String,
                            LPI_KEY: String,
                            LEVEL: Option[String],
                            ORGANISATION: String,
                            SAO_START_NUMBER: Option[String],
                            SAO_START_SUFFIX: Option[String],
                            SAO_END_NUMBER: Option[String],
                            SAO_END_SUFFIX: Option[String],
                            PAO_START_NUMBER: Option[String],
                            PAO_START_SUFFIX: Option[String],
                            PAO_END_NUMBER: Option[String],
                            PAO_END_SUFFIX: Option[String],
                            STREET_DESCRIPTION: String,
                            LOCALITY_NAME: Option[String],
                            TOWN_NAME: Option[String])

object LPI {
  implicit val formatLPI = Json.format[LPI]
}