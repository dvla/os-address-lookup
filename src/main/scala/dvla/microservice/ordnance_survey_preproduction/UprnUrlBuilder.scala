package dvla.microservice.ordnance_survey_preproduction

import dvla.microservice.Configuration
import dvla.domain.address_lookup.UprnToAddressLookupRequest

final class UprnUrlBuilder(val configuration: Configuration){
  private val apiKey = configuration.apiKey
  private val baseUrl = configuration.baseUrl

  def endPoint(request: UprnToAddressLookupRequest) = s"$baseUrl/uprn?uprn=${request.uprn}&dataset=dpa&key=$apiKey"
}
