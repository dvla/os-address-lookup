package dvla.domain.ordnance_survey_beta_0_6

import java.net.URI

import play.api.libs.json.{JsString, JsSuccess, JsValue, Json, Reads, Writes}

final case class Header(uri: URI,
                        query: String,
                        offset: Int,
                        totalresults: Int,
                        format: String,
                        dataset: String,
                        maxresults: Int)

object Header {

  implicit val uriReads: Reads[URI] = new Reads[URI] {
    override def reads(json: JsValue) = JsSuccess(new URI(json.as[String]))
  }

  implicit val uriWrites: Writes[URI] = new Writes[URI] {
    override def writes(uri: URI) = JsString(uri.toString)
  }

  implicit val formatHeader = Json.format[Header]
}