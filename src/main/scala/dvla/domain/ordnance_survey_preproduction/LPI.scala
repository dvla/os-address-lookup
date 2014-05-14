package dvla.domain.ordnance_survey_preproduction

import play.api.libs.json.Json

case class LPI(UPRN: String,
                            ADDRESS: String,
                            USRN: String,
                            LPI_KEY: String,
                            PAO_START_NUMBER: String,
                            STREET_DESCRIPTION: String,
                            TOWN_NAME: String,
                            ADMINISTRATIVE_AREA: String,
                            AREA_NAME: String,
                            POSTCODE_LOCATOR: String,
                            RPC: String,
                            X_COORDINATE: String,
                            Y_COORDINATE: String,
                            STATUS: String,
                            MATCH: String,
                            MATCH_DESCRIPTION: String)

object LPI {
  implicit val formatLPI = Json.format[LPI]
}