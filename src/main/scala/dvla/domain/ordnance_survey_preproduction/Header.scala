package dvla.domain.ordnance_survey_preproduction

import java.net.URI
import play.api.libs.json._

final case class Header(uri: URI,
                               /*query: String,*/
                               offset: Int,
                               totalresults: Int/*,
                               format: String,
                               dataset: String,
                               maxresults: Int*/)

object Header {
  implicit val uriReads: Reads[URI] = new Reads[URI] {
    override def reads(json: JsValue) = JsSuccess(new URI(json.as[String]))
  }

  implicit val formatHeader = Json.reads[Header]
}
