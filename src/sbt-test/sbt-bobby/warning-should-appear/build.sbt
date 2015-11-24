//enablePlugins(GitVersioning)
//enablePlugins(Bobb)
enablePlugins(SbtBobbyPlugin)

import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

jsonOutputFileOverride := Some("/tmp/bobby-json-out.json")

scalaVersion := "2.11.6"

libraryDependencies := Seq(
  "uk.gov.hmrc" %% "play-health" % "0.1.0",
  "uk.gov.hmrc" %% "play-filters" % "0.1.0")

deprecatedDependenciesUrl := Some(file("dependencies.json").toURL)

val checkVersion = taskKey[Unit]("checks the version is the tag versino")
checkVersion := {

  val json = read(file("/tmp/bobby-json-out.json"))
  println(json)
  assert(json.contains("play-health"), "Did not find a reference to play-health in the generated file")
  assert(!json.contains("play-filters"), "Found a reference to play-filters when we shouldn't have")
//  val v = version.value
//  val prop = sys.props("project.version")
//  assert(v == prop, s"Failed to set version to environment variable.  Found: $v, Expected: $prop")
}
