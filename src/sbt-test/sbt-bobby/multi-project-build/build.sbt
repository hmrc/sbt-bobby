import play.api.libs.json.Json
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val global = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.20",
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
  ).aggregate(common, sub)

lazy val common = project
  .settings(
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    libraryDependencies := Seq(
    "org.scalacheck"       %% "scalacheck" % "1.14.3",
  ))

lazy val sub = project
  .settings(
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    libraryDependencies := Seq(
      "org.reactivemongo" %% "reactivemongo" % "0.17.0"
    ),
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    TaskKey[Unit]("check") := {
      println(s">>>> In check!")
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
