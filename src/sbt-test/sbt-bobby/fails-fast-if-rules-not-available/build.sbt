import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.13.16",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    libraryDependencies := Seq(
      "org.reactivemongo" %% "reactivemongo" % "1.0.10"
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
  )
