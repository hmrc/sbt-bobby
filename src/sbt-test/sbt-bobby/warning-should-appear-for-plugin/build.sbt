import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

val checkVersion = taskKey[Unit]("checks the version is the tag version")

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.10",
    libraryDependencies := Seq(
      "uk.gov.hmrc" %% "play-health" % "0.1.0",
      "uk.gov.hmrc" %% "play-filters" % "0.1.0"
    ),
    jsonOutputFileOverride := Some("/tmp/bobby-json-out.json"),
    deprecatedDependenciesUrl := Some(file("dependencies.json").toURI.toURL),

    checkVersion := {

      val json = read(file("/tmp/bobby-json-out.json"))
      println(json)
      assert(json.contains("expires"), "Did not show warning for the plugin")

    }
  )
