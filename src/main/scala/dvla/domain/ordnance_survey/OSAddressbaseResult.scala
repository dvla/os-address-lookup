package dvla.domain.ordnance_survey

import play.api.libs.json.Json

case class OSAddressbaseResult(
                                DPA: Option[OSAddressbaseDPA],
                                LPI: Option[OSAddressbaseLPI]
                                )

object OSAddressbaseResult {
  implicit val readsOSAddressbaseResult = Json.format[OSAddressbaseResult]
}