package dvla.domain.ordnance_survey_preproduction

import java.net.URI

import play.api.libs.json.{JsSuccess, JsValue, Json, Reads}

final case class Header(uri: URI,
                        offset: Int,
                        totalresults: Int)

object Header {
  implicit val uriReads: Reads[URI] = new Reads[URI] {
    override def reads(json: JsValue) = JsSuccess(new URI(json.as[String]))
  }

  implicit val formatHeader = Json.reads[Header]
}