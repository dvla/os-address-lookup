package dvla.domain.address_lookup

final case class AddressResponseDto(address: String,
                                    uprn: Option[String],
                                    businessName: Option[String])