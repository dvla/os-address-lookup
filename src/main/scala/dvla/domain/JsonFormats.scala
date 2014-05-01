package dvla.domain

import spray.httpx.SprayJsonSupport
import spray.json._
import dvla.domain.address_lookup.UprnAddressPair
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnToAddressLookupRequest
import dvla.domain.address_lookup.UprnToAddressResponse
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.domain.address_lookup.AddressViewModel

object JsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val addressModelViewFormat = jsonFormat2(AddressViewModel)
  implicit val uprnAddressPairFormat = jsonFormat2(UprnAddressPair)
  implicit val postcodeToAddressResponseFormat = jsonFormat1(PostcodeToAddressResponse)
  implicit val uprnToAddressResponseFormat = jsonFormat1(UprnToAddressResponse)
  implicit val postcodeToAddressLookupRequestFormat = jsonFormat1(PostcodeToAddressLookupRequest)
  implicit val uprnToAddressLookupRequestFormat = jsonFormat1(UprnToAddressLookupRequest)

}
