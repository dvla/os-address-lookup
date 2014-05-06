package dvla.domain.ordnance_survey

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class OSAddressbaseDPA(UPRN: String,
                            address: String,
                            poBoxNumber: Option[String] = None,
                            organisationName: Option[String] = None,
                            departmentName: Option[String] = None,
                            subBuildingName: Option[String] = None,
                            buildingName: Option[String] = None,
                            buildingNumber: Option[String] = None,
                            dependentThoroughfareName: Option[String] = None,
                            thoroughfareName: Option[String] = None,
                            doubleDependentLocality: Option[String] = None,
                            dependentLocality: Option[String] = None,
                            postTown: String,
                            postCode: String,
                            RPC: String,
                            xCoordinate: Float,
                            yCoordinate: Float,
                            status: String,
                            matchScore: Float,
                            matchDescription: String)

object OSAddressbaseDPA {
  implicit val reads: Reads[OSAddressbaseDPA] = (
    (__ \ "UPRN").read[String] and
      (__ \ "ADDRESS").read[String] and
      (__ \ "PO_BOX_NUMBER").readNullable[String] and
      (__ \ "ORGANISATION_NAME").readNullable[String] and
      (__ \ "DEPARTMEMT_NAME").readNullable[String] and
      (__ \ "SUB_BUILDING_NAME").readNullable[String] and
      (__ \ "BUILDING_NAME").readNullable[String] and
      (__ \ "BUILDING_NUMBER").readNullable[String] and
      (__ \ "DEPENDENT_THOROUGHFARE_NAME").readNullable[String] and
      (__ \ "THOROUGHFARE_NAME").readNullable[String] and
      (__ \ "DOUBLE_DEPENDENT_LOCALITY").readNullable[String] and
      (__ \ "DEPENDENT_LOCALITY").readNullable[String] and
      (__ \ "POST_TOWN").read[String] and
      (__ \ "POSTCODE").read[String] and
      (__ \ "RPC").read[String] and
      (__ \ "X_COORDINATE").read[Float] and
      (__ \ "Y_COORDINATE").read[Float] and
      (__ \ "STATUS").read[String] and
      (__ \ "MATCH").read[Float] and
      (__ \ "MATCH_DESCRIPTION").read[String]
    )(OSAddressbaseDPA.apply _)

  implicit val implicitWrites = new Writes[OSAddressbaseDPA] {
    def writes(osAddressbaseDPA: OSAddressbaseDPA): JsValue = Json.obj(
      "UPRN" -> osAddressbaseDPA.UPRN,
      "ADDRESS" -> osAddressbaseDPA.address,
      "PO_BOX_NUMBER" -> osAddressbaseDPA.poBoxNumber,
      "ORGANISATION_NAME" -> osAddressbaseDPA.organisationName,
      "DEPARTMEMT_NAME" -> osAddressbaseDPA.departmentName,
      "SUB_BUILDING_NAME" -> osAddressbaseDPA.subBuildingName,
      "BUILDING_NAME" -> osAddressbaseDPA.buildingName,
      "BUILDING_NUMBER" -> osAddressbaseDPA.buildingNumber,
      "DEPENDENT_THOROUGHFARE_NAME" -> osAddressbaseDPA.dependentThoroughfareName,
      "THOROUGHFARE_NAME" -> osAddressbaseDPA.thoroughfareName,
      "DOUBLE_DEPENDENT_LOCALITY" -> osAddressbaseDPA.doubleDependentLocality,
      "DEPENDENT_LOCALITY" -> osAddressbaseDPA.dependentLocality,
      "POST_TOWN" -> osAddressbaseDPA.postTown,
      "POSTCODE" -> osAddressbaseDPA.postCode,
      "RPC" -> osAddressbaseDPA.RPC,
      "X_COORDINATE" -> osAddressbaseDPA.xCoordinate,
      "Y_COORDINATE" -> osAddressbaseDPA.yCoordinate,
      "STATUS" -> osAddressbaseDPA.status,
      "MATCH" -> osAddressbaseDPA.matchScore,
      "MATCH_DESCRIPTION" -> osAddressbaseDPA.matchDescription)
  }
}
