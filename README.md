DVLA Ordnance Survey Service
============================

`ordnance-survey` is a [microservice][microservices] façade for the [Ordnance Survey API][ordnance-survey], encapsulating organisation-wide configuration such as authentication and licensing.

The codebase is predominantly [Scala][scala] and is implemented against [Spray][spray]: a lightweight HTTP framework for the JVM.

Running the application
-----------------------

    cd ordnance-survey
    sbt run

Usage
=====

Get all addresses in a postcode
-------------------------------

To see the raw json returned by the ordnance-survey service, go to this url with a web browser:

    https://api.ordnancesurvey.co.uk/places/v1/addresses/postcode?postcode=<INSERT POSTCODE WITHOUT SPACES>&dataset=dpa&key=<INSERT API KEY>

To see how this micro-service will re-format that json to a view model, you can send a curl command the micro-service:

    curl http://localhost:8083/postcode-to-address?postcode=<INSERT POSTCODE WITHOUT SPACES>

Get one address for a UPRN
--------------------------

To see the raw json returned by the ordnance-survey service, go to this url with a web browser:

    https://api.ordnancesurvey.co.uk/places/v1/addresses/uprn?uprn=<INSERT UPRN>&dataset=dpa&key=<INSERT API KEY>

To see how this micro-service will re-format that json to a view model, you can send a curl command the micro-service:

    curl http://localhost:8083/uprn-to-address?uprn=<INSERT UPRN>

[microservices]: http://martinfowler.com/articles/microservices.html "Microservices"
[ordnance-survey]: www.ordnancesurvey.co.uk "Ordnance Survey"
[scala]: http://www.scala-lang.org/ "Scala Language"
[spray]: http://spray.io/ "Spray"
