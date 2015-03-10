/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import sbt._
import Keys._

object PluginBuild extends Build {

  val pluginName = "bobby"
  val pluginVersion = "0.4.0"

  lazy val root = Project(pluginName, base = file("."), settings = Seq(
    version := pluginVersion,
    sbtPlugin := true,
    organization := "uk.gov.hmrc",
    name := pluginName,
    scalaVersion := "2.10.4",
    resolvers ++= Seq(
      Opts.resolver.sonatypeReleases,
      Opts.resolver.sonatypeSnapshots
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.4.0-M1",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    ),
    publishArtifact := true,
    publishArtifact in Test := false
  ) ++ ArtefactDescription()
  )
}


object ArtefactDescription {

  def apply() = Seq(
      pomExtra := (<url>https://www.gov.uk/government/organisations/hm-revenue-customs</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git@github.com:hmrc/bobby.git</connection>
          <developerConnection>scm:git@github.com:hmrc/bobby.git</developerConnection>
          <url>git@github.com:hmrc/bobby.git</url>
        </scm>
        <developers>
          <developer>
            <id>charleskubicek</id>
            <name>Charles Kubicek</name>
            <url>http://www.equalexperts.com</url>
          </developer>
          <developer>
            <id>duncancrawford</id>
            <name>Duncan Crawford</name>
            <url>http://www.equalexperts.com</url>
          </developer>
        </developers>)
    )

}
