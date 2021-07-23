import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.8",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    libraryDependencies := Seq(
      "uk.gov.hmrc"       %% "simple-reactivemongo" % "7.20.0-play-26",
      "org.reactivemongo" %% "reactivemongo" % "0.17.0"
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
  )
