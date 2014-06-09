package dvla.microservice.ordnance_survey_preproduction

import dvla.microservice.Configuration
import dvla.domain.address_lookup.PostcodeToAddressLookupRequest

final class PostcodeUrlBuilder(val configuration: Configuration){
  private val apiKey = configuration.apiKey
  private val baseUrl = configuration.baseUrl

  def endPoint(request: PostcodeToAddressLookupRequest) = s"$baseUrl/postcode?postcode=${postcodeWithNoSpaces(request.postcode)}&dataset=dpa&key=$apiKey"

  private def postcodeWithNoSpaces(postcode: String): String = postcode.filter(_ != ' ')
}
