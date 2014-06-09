package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.microservice.Configuration

final class PostcodeUrlBuilder(val configuration: Configuration) {
  private val apiKey = configuration.apiKey
  private val baseUrl = configuration.baseUrl

  def endPoint(request: PostcodeToAddressLookupRequest) = {
    val languageCode = request.languageCode match {
      case Some(lang) => "&lr=" + lang
      case None => ""
    }
    s"$baseUrl/postcode?" +
      s"postcode=${postcodeWithNoSpaces(request.postcode)}" +
      s"&dataset=dpa" +
      languageCode +
      s"&key=$apiKey"
  }

  private def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')
}
