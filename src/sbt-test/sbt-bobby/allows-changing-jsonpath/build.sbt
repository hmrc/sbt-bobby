import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    deprecatedDependenciesUrl := Some(file("dependencies.json").toURI.toURL),
    jsonOutputFileOverride := Some("target/changed-name-report.json"),
  )
