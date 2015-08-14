package dvla.domain

import dvla.domain.address_lookup.AddressDto
import dvla.domain.address_lookup.AddressViewModel
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.UprnToAddressResponse
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object JsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val addressModelViewFormat = jsonFormat2(AddressViewModel)
  implicit val uprnAddressPairFormat = jsonFormat2(UprnAddressPair)
  implicit val postcodeToAddressResponseFormat = jsonFormat1(PostcodeToAddressResponse)
  implicit val uprnToAddressResponseFormat = jsonFormat1(UprnToAddressResponse)
  implicit val addressDtoFormat = jsonFormat7(AddressDto)
}
