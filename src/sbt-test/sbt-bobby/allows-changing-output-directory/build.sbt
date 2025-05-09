import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.13.16",
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    outputDirectoryOverride := Some("target/changed-dir")
  )
