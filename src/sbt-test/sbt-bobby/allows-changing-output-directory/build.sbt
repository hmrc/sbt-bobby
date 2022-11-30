import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    outputDirectoryOverride := Some("target/changed-dir"),
  )
