package dvla.domain

import spray.json._
import spray.httpx.SprayJsonSupport
import dvla.domain.address_lookup._
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest

object JsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val addressModelViewFormat = jsonFormat2(AddressViewModel)
  implicit val uprnAddressPairFormat = jsonFormat2(UprnAddressPair)
  implicit val postcodeToAddressResponseFormat = jsonFormat1(PostcodeToAddressResponse)
  implicit val uprnToAddressResponseFormat = jsonFormat1(UprnToAddressResponse)
  implicit val postcodeToAddressLookupRequestFormat = jsonFormat1(PostcodeToAddressLookupRequest)
  implicit val uprnToAddressLookupRequestFormat = jsonFormat1(UprnToAddressLookupRequest)

}
