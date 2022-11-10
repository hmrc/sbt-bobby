import play.api.libs.json.Json
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.10",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),

    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report-project-compile.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet

      assert(reasons.contains("bad auto build"), "Did not find expected violations")
      ()
    }
  )
