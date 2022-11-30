import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    TaskKey[Unit]("check") := {
      if (bobbyViewType.value != uk.gov.hmrc.bobby.output.Nested) sys.error("view type was not Nested as expected")
      if (!bobbyStrictMode.value) sys.error("strict mode was not true as expected")
      if (bobbyConsoleColours.value) sys.error("console colours was not false as expected")
      ()
    }
  )
