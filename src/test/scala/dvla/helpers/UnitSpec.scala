package dvla.helpers

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures

abstract class UnitSpec extends WordSpec with Matchers with ScalaFutures
