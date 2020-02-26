import play.api.libs.json.Json
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.10",
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    deprecatedDependenciesUrl := Some(file("bobby-rules.json").toURI.toURL),

    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet

      assert(reasons.contains("bad auto build"), "Did not find expected violations")
      ()
    }
  )
