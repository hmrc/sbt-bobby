import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    deprecatedDependenciesUrl := Some(file("dependencies.json").toURI.toURL),
    TaskKey[Unit]("check") := {
      val viewType = bobbyViewType.value
      if (viewType != uk.gov.hmrc.bobby.output.Nested) sys.error("view type was not Nested as expected")
      ()
    }
  )
