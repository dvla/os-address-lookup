package dvla.microservice

import scala.concurrent.Future
import dvla.domain.address_lookup.{AddressLookupRequest, PostcodeToAddressResponse}

trait AddressLookupCommand {

  val configuration: Configuration

  def apply(request: AddressLookupRequest): Future[PostcodeToAddressResponse]
}
