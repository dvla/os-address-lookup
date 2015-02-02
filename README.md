DVLA Ordnance Survey Service
============================

`ordnance-survey` is a [microservice][microservices] façade for the [Ordnance Survey API][ordnance-survey], encapsulating organisation-wide configuration such as authentication and licensing.

The codebase is predominantly [Scala][scala] and is implemented against [Spray][spray]: a lightweight HTTP framework for the JVM.

Running the application
-----------------------

    cd ordnance-survey
    sbt run

Usage
-----
To get all addresses in a postcode:

    `curl http://localhost:8083/postcode-to-address?postcode=`

followed by a real postcode without spaces.

To get one address for a UPRN:

    curl http://localhost:8083/uprn-to-address?uprn=

followed by a UPRN.


[microservices]: http://martinfowler.com/articles/microservices.html "Microservices"
[ordnance-survey]: www.ordnancesurvey.co.uk "Ordnance Survey"
[scala]: http://www.scala-lang.org/ "Scala Language"
[spray]: http://spray.io/ "Spray"
