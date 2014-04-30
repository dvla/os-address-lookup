package dvla.domain

import spray.json._
import spray.httpx.SprayJsonSupport
import dvla.domain.address_lookup._
import dvla.domain.address_lookup.AddressLookupRequest

object JsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val uprnAddressPairFormat = jsonFormat2(UprnAddressPair)
  implicit val postcodeToAddressResponseFormat = jsonFormat1(PostcodeToAddressResponse)
  implicit val vehicleLookupRequestFormat = jsonFormat1(AddressLookupRequest)

}
