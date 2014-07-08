package dvla.microservice

import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.UprnToAddressLookupRequest
import dvla.domain.address_lookup.UprnToAddressResponse
import scala.concurrent.Future

trait AddressLookupCommand {

  val configuration: Configuration
  def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse]
  def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse]
}
