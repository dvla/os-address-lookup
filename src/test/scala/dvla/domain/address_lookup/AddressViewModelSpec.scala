package dvla.domain.address_lookup

import dvla.helpers.UnitSpec

class AddressViewModelSpec extends UnitSpec {

  "AddressViewModel - model" should {
    "handle ordnance_survey uprn of the correct size" in {
      val keeperUprnValid = 10123456789L
      val address = AddressViewModel(address = Seq("line1", "line2", "line2"))

      address.address.size should equal(3)
    }
  }
}