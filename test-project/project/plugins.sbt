resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

// Below is add add a dependency on the main outer project, sbt-bobby itself
val location = file("..").toURI

val sbtBobby = RootProject(location)

val root = project.in(file(".")).dependsOn(sbtBobby)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.2.0")
