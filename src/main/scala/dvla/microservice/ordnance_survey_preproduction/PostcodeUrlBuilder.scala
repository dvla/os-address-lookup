package dvla.microservice.ordnance_survey_preproduction

import dvla.domain.address_lookup.PostcodeToAddressLookupRequest
import dvla.microservice.Configuration

final class PostcodeUrlBuilder(val configuration: Configuration) {
  private val apiKey = configuration.apiKey
  private val baseUrl = configuration.baseUrl

  def endPoint(request: PostcodeToAddressLookupRequest): String = {
    val languageCode = request.languageCode match {
      // Collapse the many variations of English (e.g. en-us, en-gb) into just the first 2 chars.
      case Some(lang) => "&lr=" + lang.toUpperCase.substring(0,2)
      case None => ""
    }
    s"$baseUrl/postcode?" +
      s"postcode=${postcodeWithNoSpaces(request.postcode)}" +
      "&dataset=dpa" +
      languageCode +
      s"&key=$apiKey"
  }

  private def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')
}