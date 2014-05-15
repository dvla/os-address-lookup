package dvla.domain.address_lookup


case class AddressViewModel(uprn: Option[Long] = None, // Optional because if user is manually entering the address they will not be allowed to enter ordnance_survey UPRN, it is only populated by address lookup services.
                            address: Seq[String])
