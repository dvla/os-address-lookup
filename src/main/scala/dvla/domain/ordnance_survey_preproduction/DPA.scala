package dvla.domain.ordnance_survey_preproduction

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DPA(UPRN: String,
                            address: String,
                            poBoxNumber: Option[String] = None,
                            organisationName: Option[String] = None,
                            buildingNumber: Option[String] = None,
                            thoroughfareName: Option[String] = None,
                            dependentLocality: Option[String] = None,
                            postTown: String,
                            postCode: String,
                            RPC: String,
                            xCoordinate: Float,
                            yCoordinate: Float,
                            status: String,
                            matchScore: Float,
                            matchDescription: String)

object DPA {
  implicit val readsDPA: Reads[DPA] = (
    (__ \ "UPRN").read[String] and
      (__ \ "ADDRESS").read[String] and
      (__ \ "PO_BOX_NUMBER").readNullable[String] and
      (__ \ "ORGANISATION_NAME").readNullable[String] and
      (__ \ "BUILDING_NUMBER").readNullable[String] and
      (__ \ "THOROUGHFARE_NAME").readNullable[String] and
      (__ \ "DEPENDENT_LOCALITY").readNullable[String] and
      (__ \ "POST_TOWN").read[String] and
      (__ \ "POSTCODE").read[String] and
      (__ \ "RPC").read[String] and
      (__ \ "X_COORDINATE").read[Float] and
      (__ \ "Y_COORDINATE").read[Float] and
      (__ \ "STATUS").read[String] and
      (__ \ "MATCH").read[Float] and
      (__ \ "MATCH_DESCRIPTION").read[String]
    )(DPA.apply _)

  implicit val writesDPA = new Writes[DPA] {
    def writes(dpa: DPA): JsValue = Json.obj(
      "UPRN" -> dpa.UPRN,
      "ADDRESS" -> dpa.address,
      "PO_BOX_NUMBER" -> dpa.poBoxNumber,
      "ORGANISATION_NAME" -> dpa.organisationName,
      "BUILDING_NUMBER" -> dpa.buildingNumber,
      "THOROUGHFARE_NAME" -> dpa.thoroughfareName,
      "DEPENDENT_LOCALITY" -> dpa.dependentLocality,
      "POST_TOWN" -> dpa.postTown,
      "POSTCODE" -> dpa.postCode,
      "RPC" -> dpa.RPC,
      "X_COORDINATE" -> dpa.xCoordinate,
      "Y_COORDINATE" -> dpa.yCoordinate,
      "STATUS" -> dpa.status,
      "MATCH" -> dpa.matchScore,
      "MATCH_DESCRIPTION" -> dpa.matchDescription)
  }
}
