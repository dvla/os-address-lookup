package dvla.microservice

import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}
import dvla.domain.address_lookup._
import akka.event.Logging
import dvla.domain.address_lookup.PostcodeToAddressResponse
import dvla.domain.address_lookup.AddressLookupRequest
import scala.Some

class CannedAddressLookupCommand(val configuration: Configuration)(implicit system: ActorSystem, executionContext: ExecutionContext) extends AddressLookupCommand {

  final lazy val log = Logging(system, this.getClass)

  override def apply(request: AddressLookupRequest) = Future[PostcodeToAddressResponse] {

    log.debug("Dealing with the post request on postcode-to-address with canned data response...")
    log.debug("... for postcode " + request.postcode)

    val traderUprnValid = 12345L
    val traderUprnValid2 = 4567L
    val addressWithUprn = AddressViewModel(uprn = Some(traderUprnValid), address = Seq("44 Hythe Road", "White City", "London", "NW10 6RJ"))

    val fetchedAddresses = Seq(
      UprnAddressPair(traderUprnValid.toString, addressWithUprn.address.mkString(", ")),
      UprnAddressPair(traderUprnValid2.toString, addressWithUprn.address.mkString(", "))
    )

    PostcodeToAddressResponse(fetchedAddresses)

  }

}

