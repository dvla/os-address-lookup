package dvla.domain.address_lookup

final case class PostcodeToAddressResponse(addresses: Seq[UprnAddressPair])