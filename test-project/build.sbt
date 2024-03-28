import play.api.libs.json.Json
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

lazy val root = (project in file("."))
  .enablePlugins(SbtBobbyPlugin)
  .settings(
    scalaVersion := "2.12.18",
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= Seq(
      "org.pegdown"   %  "pegdown"                  % "1.3.0",
      "org.jsoup"     %  "jsoup"                    % "1.12.1",
      "uk.gov.hmrc"   %% "simple-reactivemongo"     % "7.13.0-play-26",
      "org.scalatest" %% "scalatest"                % "3.0.0"         % Test
    ),
    bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
  )
