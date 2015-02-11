package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup.UprnToAddressLookupRequest
import dvla.microservice.Configuration

final class UprnUrlBuilder(val configuration: Configuration){
  private val apiKey = configuration.apiKey
  private val baseUrl = configuration.baseUrl

  def endPoint(request: UprnToAddressLookupRequest): String = {
    val languageCode = request.languageCode match {
      case Some(lang) => "&lr=" + lang
      case None => ""
    }
    s"$baseUrl/uprn?" +
      s"uprn=${request.uprn}" +
      s"&dataset=dpa" +
      languageCode +
      s"&key=$apiKey"
  }
}
