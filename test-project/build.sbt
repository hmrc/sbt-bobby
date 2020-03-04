import play.api.libs.json.Json
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.10",
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest"                % "3.0.0",
      "org.pegdown"   % "pegdown"                   % "1.3.0",
      "org.jsoup"     % "jsoup"                     % "1.12.1",
      "uk.gov.hmrc"   %% "simple-reactivemongo"     % "7.13.0-play-26",
      "org.scalatest" %% "scalatest"                % "3.0.0" % "test"
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL),
  )

