package dvla.microservice

import dvla.domain.address_lookup._
import scala.concurrent.Future

trait AddressLookupCommand {

  def apply(request: PostcodeToAddressLookupRequest): Future[PostcodeToAddressResponse]
  def applyDetailedResult(request: PostcodeToAddressLookupRequest): Future[Seq[AddressDto]]
  def apply(request: UprnToAddressLookupRequest): Future[UprnToAddressResponse]
}
