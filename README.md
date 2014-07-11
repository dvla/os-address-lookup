Master [![Build Status](https://travis-ci.org/dvla/os-address-lookup.svg?branch=master)](https://travis-ci.org/dvla/os-address-lookup)

DVLA Ordnance Survey Service
============================

`ordnance-survey` is a [microservice][microservices] façade for the [Ordnance Survey API][ordnance-survey], encapsulating organisation-wide configuration such as authentication and licensing.

The codebase is predominantly [Scala][scala] and is implemented against [Spray][spray]: a lightweight HTTP framework for the JVM.

Running the application
-----------------------

    cd ordnance-survey
    sbt run

[microservices]: http://martinfowler.com/articles/microservices.html "Microservices"
[ordnance-survey]: www.ordnancesurvey.co.uk "Ordnance Survey"
[scala]: http://www.scala-lang.org/ "Scala Language"
[spray]: http://spray.io/ "Spray"
