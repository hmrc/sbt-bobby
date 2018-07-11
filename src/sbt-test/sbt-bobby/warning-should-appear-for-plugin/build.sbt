//enablePlugins(GitVersioning)
//enablePlugins(Bobb)
enablePlugins(SbtBobbyPlugin)

import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

jsonOutputFileOverride := Some("/tmp/bobby-json-out.json")

scalaVersion := "2.11.6"

libraryDependencies := Seq("uk.gov.hmrc" %% "play-health" % "0.1.0", "uk.gov.hmrc" %% "play-filters" % "0.1.0")

deprecatedDependenciesUrl := Some(file("dependencies.json").toURL)

val checkVersion = taskKey[Unit]("checks the version is the tag version")
checkVersion := {

  val json = read(file("/tmp/bobby-json-out.json"))
  println(json)
  assert(json.contains("expires"), "Did not show warning for the plugin")

  // play-filters currently appears in the list because it can't be found in a repository
  //  assert(!json.contains("play-filters"), "Found a reference to play-filters when we shouldn't have")

}
