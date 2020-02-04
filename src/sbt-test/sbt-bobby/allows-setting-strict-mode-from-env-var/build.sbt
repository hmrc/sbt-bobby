import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    TaskKey[Unit]("check") := {
      val viewType = bobbyStrictMode.value
      if (viewType != true) sys.error("strict mode was not true as expected")
      ()
    }
  )
