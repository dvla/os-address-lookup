package dvla.domain.address_lookup

final case class PostcodeToAddressResponse(addresses: Seq[UprnAddressPair])

case class AddressDto(addressLine: String,
                      businessName: Option[String],
                      streetAddress1: String,
                      streetAddress2: Option[String],
                      streetAddress3: Option[String],
                      postTown: String,
                      postCode: String)

final case class Addresses(addresses: Seq[AddressDto])