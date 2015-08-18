package dvla.domain.ordnance_survey_preproduction

import dvla.helpers.UnitSpec
import java.net.URI
import play.api.libs.json.Json
import scala.io.Source

class ResponseSpec extends UnitSpec {

  "Response Parser loading json for ec1a 4jq" should {
    "populate the header given json with header but 0 results" in {
      val resp = getResource(Path + "Empty_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(0)
    }

    "populate the the results given json with 0 result" in {
      val resp = getResource(Path + "Empty_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case None =>
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and 1 result DPA only" in {
      val resp = getResource(Path + "One_DPA_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(1)
    }

    "populate the the results given json with 1 result DPA only" in {
      val resp = getResource(Path + "One_DPA_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(1)
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and multiple results" in {
      val resp = getResource(Path + "Multiple_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(3)
    }

    "populate the the results given json with multiple results" in {
      val resp = getResource(Path + "Multiple_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(3)
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and 1 result DPA and LPI" in {
      val resp = getResource(Path + "One_DPA_And_LPI_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=200010019924&key=[INSERT_USER_API_KEY_HERE]"))
      poso.header.totalresults should equal(2)
    }

    "populate the the results given json with 1 result DPA and LPI" in {
      val resp = getResource(Path + "One_DPA_And_LPI_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(2)
        case _ => fail("expected results")
      }
    }
  }

  private final val Path = "ordnance_survey_preproduction/"

  private def getResource(name: String) = Source.fromURL(this.getClass.getResource(s"/$name")).mkString("")
}