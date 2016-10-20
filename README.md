DVLA Ordnance Survey Service
============================

`ordnance-survey` is a [microservice][microservices] façade for the [Ordnance Survey API][ordnance-survey], encapsulating organisation-wide configuration such as authentication and licensing.

The codebase is predominantly [Scala][scala] and is implemented against [Spray][spray]: a lightweight HTTP framework for the JVM.

The application is calling the ordnancesurvey.co.uk service to retrieve addresses from a postcode. 

Running the application
-----------------------

	The application is running as part of an exemplar by executing the following command on the exemplar:

    	sbt sandbox

    To run this microservice on its own, you have to create a fat jar and then run this jar. To do so:

    	sbt assembly
    	java -jar target/scala-2.11/os-address-lookup-XXX.jar

    	where XXX is the current version

    The listening port is defined in src/main/resources/application.conf, under the `port` property 


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

Useful Links
============

[Postcode Address File specification](http://www.poweredbypaf.com/wp-content/uploads/2015/02/Latest-Programmers_guide_Edition-7-Version-6.pdf)

[Some interesting false assumptions about addresses](https://www.mjt.me.uk/posts/falsehoods-programmers-believe-about-addresses/)

[OS Datasets](http://data.ordnancesurvey.co.uk/)

[OS REST API test page](https://apidocs.os.uk/docs/os-places-find)

Glossary
========

**DPA**  Delivery Point Address (dataset)

**LPI** Local Property Identifier (dataset)
