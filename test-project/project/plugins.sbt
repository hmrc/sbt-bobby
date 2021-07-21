resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

// Below is to add a dependency on the main outer project, sbt-bobby itself
val location = file("..").toURI

val sbtBobby = RootProject(location)

val root = project.in(file(".")).dependsOn(sbtBobby)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.6.0")

// Added just to trigger a test bobby rule
addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "4.1.0")
