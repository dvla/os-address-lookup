package dvla.domain.address_lookup

final case class PostcodeToAddressLookupRequest(postcode: String,
                                                languageCode: Option[String] = None,
                                                showBusinessName: Option[Boolean] = None)