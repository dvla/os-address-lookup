package dvla.domain.address_lookup

final case class UprnToAddressLookupRequest(uprn: Long, languageCode: Option[String] = None)
