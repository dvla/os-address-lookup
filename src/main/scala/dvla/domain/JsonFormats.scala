package dvla.domain

import dvla.domain.address_lookup.{AddressDto, AddressViewModel}
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object JsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val addressModelViewFormat = jsonFormat1(AddressViewModel)
  implicit val addressDtoFormat = jsonFormat7(AddressDto)
}
