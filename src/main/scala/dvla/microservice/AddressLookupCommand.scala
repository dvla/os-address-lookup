package dvla.microservice

import scala.concurrent.Future
import dvla.domain.address_lookup.{UprnToAddressResponse, UprnToAddressLookupRequest, PostcodeToAddressLookupRequest, PostcodeToAddressResponse}

trait AddressLookupCommand {

  val configuration: Configuration

  def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse]
  def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse]

}
