package dvla.microservice

import dvla.common.LogFormats.DVLALogger
import dvla.common.clientsidesession.TrackingId
import dvla.domain.address_lookup._
import scala.concurrent.Future

trait AddressLookupCommand extends DVLALogger {

  def apply(request: PostcodeToAddressLookupRequest)(implicit trackingId: TrackingId): Future[PostcodeToAddressResponse]
  def applyDetailedResult(request: PostcodeToAddressLookupRequest)(implicit trackingId: TrackingId): Future[Seq[AddressDto]]
  def apply(request: UprnToAddressLookupRequest)(implicit trackingId: TrackingId): Future[UprnToAddressResponse]
}
