enablePlugins(SbtBobbyPlugin)

import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import sbt.IO._

scalaVersion := "2.11.6"

resolvers += Resolver.bintrayRepo("hmrc", "releases")

deprecatedDependenciesUrl := Some(file("dependencies.json").toURL)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"                % "1.2.0",
  "org.pegdown"   % "pegdown"                   % "1.3.0",
  "org.jsoup"     % "jsoup"                     % "1.6.0",
  "uk.gov.hmrc"   %% "play-reactivemongo"       % "3.2.0",
  "uk.gov.hmrc"   %% "simple-reactivemongo"     % "2.1.0",
  "uk.gov.hmrc"   %% "microservice-bootstrap"   % "2.0.0",
  "uk.gov.hmrc"   %% "play-url-binders"         % "1.0.0",
  "uk.gov.hmrc"   %% "play-config"              % "2.0.0",
  "uk.gov.hmrc"   %% "domain"                   % "2.11.0",
  "uk.gov.hmrc"   %% "play-health"              % "1.0.0",
  "uk.gov.hmrc"   %% "frontend-bootstrap"       % "1.2.1",
  "uk.gov.hmrc"   %% "play-partials"            % "1.6.0",
  "uk.gov.hmrc"   %% "play-authorised-frontend" % "1.2.0",
  "uk.gov.hmrc"   %% "play-config"              % "1.0.0",
  "uk.gov.hmrc"   %% "play-json-logger"         % "1.0.0",
  "uk.gov.hmrc"   %% "play-ui"                  % "1.11.0",
  "uk.gov.hmrc"   %% "url-builder"              % "0.6.0",
  "org.scalatest" %% "scalatest"                % "2.2.4" % "test",
  "uk.gov.hmrc"   %% "http-caching-client"      % "4.0.0"
)
