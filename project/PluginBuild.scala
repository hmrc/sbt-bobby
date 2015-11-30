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

import _root_.sbtassembly.AssemblyKeys._
import _root_.sbtassembly.AssemblyKeys._
import _root_.sbtassembly.MergeStrategy
import _root_.sbtassembly.PathList
import sbtassembly.AssemblyKeys._
import sbtassembly._

import sbt.ScriptedPlugin._
import sbt._
import Keys._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object PluginBuild extends Build {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
  import com.typesafe.sbt.GitVersioning
  import com.typesafe.sbt.SbtGit.git

  val pluginName = "sbt-bobby"

  lazy val root = (project in file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      sbtPlugin := true,
      name := pluginName,
      targetJvm := "jvm-1.7",
      scalaVersion := "2.10.4",
      libraryDependencies ++= Seq(
        "commons-codec" % "commons-codec" % "1.10",
        "joda-time" % "joda-time" % "2.9.1",
        "org.joda" % "joda-convert" % "1.8.1",
        "com.typesafe.play" %% "play-json" % "2.3.10" % "test",
        "org.scalatest" %% "scalatest" % "2.2.4" % "test",
        "org.pegdown" % "pegdown" % "1.5.0" % "test"
      ),
      AssemblySettings(),
      addArtifact(artifact in (Compile, assembly), assembly)
    )

    .settings(ScriptedPlugin.scriptedSettings: _*)
    .settings(scriptedLaunchOpts += s"-Dproject.version=${version.value}")
}

object AssemblySettings{
  def apply()= Seq(
    assemblyJarName in assembly := "bobby.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
      case PathList("play", "core", "server", xs@_*) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in(Compile, assembly) := {
      val art = (artifact in(Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    }
  )
}

