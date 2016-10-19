package dvla.microservice

final case class Configuration(username: String = "",
                               password: String = "",
                               baseUrl: String = "",
                               apiKey: String = "",
                               addressLinesV2: Boolean = true)