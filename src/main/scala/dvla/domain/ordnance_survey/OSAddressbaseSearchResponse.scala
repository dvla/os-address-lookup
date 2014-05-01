package dvla.domain.ordnance_survey

import play.api.libs.json.Json

case class OSAddressbaseSearchResponse(
                                        header: OSAddressbaseHeader,
                                        results: Option[Seq[OSAddressbaseResult]]
                                        )

object OSAddressbaseSearchResponse {
  implicit val readsOSAddressbaseSearchResponse = Json.format[OSAddressbaseSearchResponse]
}