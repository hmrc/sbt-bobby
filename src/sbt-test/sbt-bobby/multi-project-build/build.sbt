import play.api.libs.json.Json
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val global = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.8"
  ).aggregate(common, sub)

lazy val common = project

lazy val sub = project
  .settings(
    deprecatedDependenciesUrl := Some(file("dependencies.json").toURI.toURL),
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet
      val expected = Set("-")

      assert(reasons == expected, "Did not find expected violations")
      ()
    }
  )
  .dependsOn(
    common
  )
