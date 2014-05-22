package dvla.domain.address_lookup

import dvla.helpers.UnitSpec


final class AddressViewModelSpec extends UnitSpec {

  "AddressViewModel - model" should {

    "handle ordnance_survey uprn of the correct size" in {
      val keeperUprnValid = 10123456789L
      val address = AddressViewModel(uprn = Some(keeperUprnValid), address = Seq("line1", "line2", "line2"))

      val actualUprn = address.uprn.get
      actualUprn should equal(keeperUprnValid)
      address.address.size should equal(3)
    }
  }
}