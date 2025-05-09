import play.api.libs.json.Json
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

ThisBuild / scalaVersion := "2.13.16"

lazy val global = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
  ).aggregate(common, sub)

lazy val common = project
  .settings(
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    libraryDependencies := Seq(
    "org.scalacheck"       %% "scalacheck" % "1.18.1",
  ))

lazy val sub = project
  .settings(
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    libraryDependencies := Seq(
      "org.reactivemongo" %% "reactivemongo" % "1.0.10"
    ),
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report-sub-compile.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet
      val expected = Set("-", "Bad reactivemongo")

      assert(reasons == expected, s"Expected violations $expected but was $reasons")
      ()
    }
  )
  .dependsOn(
    common
  )
