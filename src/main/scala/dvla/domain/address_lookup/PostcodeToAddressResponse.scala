package dvla.domain.address_lookup

case class AddressDto(addressLine: String,
                      businessName: Option[String],
                      streetAddress1: String,
                      streetAddress2: Option[String],
                      streetAddress3: Option[String],
                      postTown: String,
                      postCode: String)