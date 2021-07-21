import play.api.libs.json.Json
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.10.7",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    libraryDependencies := Seq(
      "uk.gov.hmrc"       %% "simple-reactivemongo" % "2.1.2"
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report-root-compile.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet
      val expected = Set("-", "Bad simple!")

      assert(reasons == expected, "Did not find expected violations")
      ()
    }
  )
