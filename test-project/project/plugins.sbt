
val location = file("..").toURI

val sbtBobby = RootProject(location)

val root = project.in(file(".")).dependsOn(sbtBobby)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.0.0")