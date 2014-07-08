package dvla.domain.ordnance_survey_beta_0_6

import scala.io.Source
import java.net.URI
import play.api.libs.json._
import dvla.helpers.UnitSpec

final class ResponseSpec extends UnitSpec {

  "Response Parser loading json for ec1a 4jq" should {

    def getResource(name: String) = Source.fromURL(this.getClass.getResource(s"/$name")).mkString("")

    "populate the header given json with header but 0 results" in {
      val resp = getResource(Path + "Empty_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("https://addressapi.ordnancesurvey.co.uk/postcode?&postcode=EC1A+4JQ&dataset=dpa&_=1392379157908"))
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

      poso.header.uri should equal(new URI("https://addressapi.ordnancesurvey.co.uk/postcode?&postcode=EC1A+4JQ&dataset=dpa&_=1392379157908"))
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

      poso.header.uri should equal(new URI("https://addressapi.ordnancesurvey.co.uk/postcode?&postcode=EC1A+4JQ&dataset=dpa&_=1392379157908"))
      poso.header.totalresults should equal(13)
    }

    "populate the the results given json with multiple results" in {
      val resp = getResource(Path + "Multiple_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.results match {
        case Some(results) => results.length should equal(13)
        case _ => fail("expected results")
      }
    }

    "populate the header given json with header and 1 result DPA and LPI" in {
      val resp = getResource(Path + "One_DPA_And_LPI_Result_Response.json")
      val poso = Json.parse(resp).as[Response]

      poso.header.uri should equal(new URI("http://addressapi.ordnancesurvey.co.uk/postcode?postcode=SO16%200AS&dataset=dpa,lpi"))
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

  private final val Path = "ordnance_survey_beta_0_6/"
}

