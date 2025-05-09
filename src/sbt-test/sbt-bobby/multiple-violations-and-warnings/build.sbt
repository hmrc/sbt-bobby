import play.api.libs.json.Json
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.13.16",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    libraryDependencies := Seq(
      "org.reactivemongo" %% "reactivemongo"  % "1.0.10",
      "commons-codec"     %  "commons-codec"  % "1.14" // This version is ok, but when evicted by the above dependency, will be rejected
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report-root-compile.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet
      val expected = Set("-", "Bad Reactivemongo", "Bad CommonsCodec", "Bad Akka")

      assert(reasons == expected, s"Expected violations $expected but was $reasons")
      ()
    }
  )
