package dvla.domain.ordnance_survey_preproduction

import scala.io.Source
import java.net.URI
import play.api.libs.json._
import dvla.helpers.UnitSpec

class ResponseSpec extends UnitSpec {

  "Response Parser loading json for ec1a 4jq" should {
    val path = "ordnance_survey_preproduction/"

    def getResource(name: String) = Source.fromURL(this.getClass.getResource(s"/$name")).mkString("")

    "populate the header given json with header but 0 results" in {
      val resp = getResource(path + "Empty_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(0)
    }

    "populate the the results given json with 0 result" in {
      val resp = getResource(path + "Empty_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case None =>
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and 1 result DPA only" in {
      val resp = getResource(path + "One_DPA_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(1)
    }

    "populate the the results given json with 1 result DPA only" in {
      val resp = getResource(path + "One_DPA_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(1)
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and multiple results" in {
      val resp = getResource(path + "Multiple_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(3)
    }

    "populate the the results given json with multiple results" in {
      val resp = getResource(path + "Multiple_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(3)
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and 1 result DPA and LPI" in {
      val resp = getResource(path + "One_DPA_And_LPI_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(2)
    }

    "populate the the results given json with 1 result DPA and LPI" in {
      val resp = getResource(path + "One_DPA_And_LPI_Result_Response.json")

      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(2)
        case _ => fail("expected results")
      }
    }
  }
}

