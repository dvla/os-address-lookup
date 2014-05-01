package dvla.domain.ordnance_survey

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class OSAddressbaseDPA(
                             UPRN: String,
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
                             matchDescription: String
                             )

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

  implicit val implicitFooWrites = new Writes[OSAddressbaseDPA] {
    def writes(foo: OSAddressbaseDPA): JsValue = Json.obj(
      "UPRN" -> foo.UPRN,
      "ADDRESS" -> foo.address,
      "PO_BOX_NUMBER" -> foo.poBoxNumber,
      "ORGANISATION_NAME" -> foo.organisationName,
      "DEPARTMEMT_NAME" -> foo.departmentName,
      "SUB_BUILDING_NAME" -> foo.subBuildingName,
      "BUILDING_NAME" -> foo.buildingName,
      "BUILDING_NUMBER" -> foo.buildingNumber,
      "DEPENDENT_THOROUGHFARE_NAME" -> foo.dependentThoroughfareName,
      "THOROUGHFARE_NAME" -> foo.thoroughfareName,
      "DOUBLE_DEPENDENT_LOCALITY" -> foo.doubleDependentLocality,
      "DEPENDENT_LOCALITY" -> foo.dependentLocality,
      "POST_TOWN" -> foo.postTown,
      "POSTCODE" -> foo.postCode,
      "RPC" -> foo.RPC,
      "X_COORDINATE" -> foo.xCoordinate,
      "Y_COORDINATE" -> foo.yCoordinate,
      "STATUS" -> foo.status,
      "MATCH" -> foo.matchScore,
      "MATCH_DESCRIPTION" -> foo.matchDescription)
  }
}
