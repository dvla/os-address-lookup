logLevel := Level.Debug

// Our plugin resolvers
resolvers += "Nexus snapshots" at "http://rep002-01.skyscape.preview-dvla.co.uk:8081/nexus/content/repositories/snapshots"

resolvers += "Nexus releases" at "http://rep002-01.skyscape.preview-dvla.co.uk:8081/nexus/content/repositories/releases"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "Maven 2" at "http://repo2.maven.org/maven2"

resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("dvla" % "build-details-generator" % "1.3.1")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// Resolver required to pick up DVLA sbt plugins that are published to bintray using ivy format
resolvers += Resolver.url(
  "dvla-sbt-plugin-releases",
   url("https://dl.bintray.com/dvla/sbt-plugin-releases"))(
       Resolver.ivyStylePatterns)
