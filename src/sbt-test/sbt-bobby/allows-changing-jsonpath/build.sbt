import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    deprecatedDependenciesUrl := Some(file("bobby-rules.json").toURI.toURL),
    jsonOutputFileOverride := Some("target/changed-name-report.json"),
  )
