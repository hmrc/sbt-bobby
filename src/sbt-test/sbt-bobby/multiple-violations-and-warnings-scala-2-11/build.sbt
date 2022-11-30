import play.api.libs.json.Json
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.11.8",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    libraryDependencies := Seq(
      "uk.gov.hmrc"       %% "simple-reactivemongo" % "7.20.0-play-26",
      "org.reactivemongo" %% "reactivemongo" % "0.17.0"
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report-root-compile.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet
      val expected = Set("-", "Mongo Test", "Bad stuff!", "Bad simple!", "Bad Joda!")

      assert(reasons == expected, "Did not find expected violations")
      ()
    }
  )
