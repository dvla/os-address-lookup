package dvla.domain.address_lookup

final case class AddressResponse(address: String,
                                 uprn: Option[String],
                                 businessName: Option[String])