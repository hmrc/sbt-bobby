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
    deprecatedDependenciesUrl := Some(file("bobby-rules.json").toURI.toURL),
    libraryDependencies := Seq(
      "uk.gov.hmrc"       %% "simple-reactivemongo" % "7.20.0-play-26",
      "org.reactivemongo" %% "reactivemongo" % "0.17.0"
    ),
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    TaskKey[Unit]("check") := {
      val json = Json.parse(read(file("target/bobby-reports/bobby-report.json")))
      val reasons = (json \\ "deprecationReason").map(_.as[String]).toSet
      val expected = Set("-", "Bad simple!")

      assert(reasons == expected, "Did not find expected violations")
      ()
    }
  )
  .dependsOn(
    common
  )
