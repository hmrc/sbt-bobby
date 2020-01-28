/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc

import net.virtualvoid.sbt.graph.ModuleGraph
import sbt.Keys._
import sbt._
import uk.gov.hmrc.bobby.{Bobby, GraphOps, ProjectPlugin}

object SbtBobbyPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object BobbyKeys {

    sealed trait Repo
    object Bintray extends Repo
    object Artifactory extends Repo
    object Nexus extends Repo
    object Maven extends Repo

    lazy val validate     = TaskKey[Unit]("validate", "Run Bobby to validate dependencies")
    lazy val repositories = SettingKey[Seq[Repo]]("repositories", "The repositories to check, in order")
    lazy val checkForLatest = SettingKey[Boolean](
      "checkForLatest",
      "Check against various repositories to compare project dependency versions against latest available")
    lazy val deprecatedDependenciesUrl =
      SettingKey[Option[URL]]("dependencyUrl", "Override the URL used to get the list of deprecated dependencies")
    lazy val jsonOutputFileOverride =
      SettingKey[Option[String]]("jsonOutputFileOverride", "Override the file used to write json result file")
  }

  import BobbyKeys._
  import net.virtualvoid.sbt.graph.DependencyGraphKeys._

  override lazy val projectSettings = Seq(
    deprecatedDependenciesUrl := None,
    jsonOutputFileOverride := None,
    parallelExecution in GlobalScope := true,
    repositories := Seq(Artifactory, Bintray),
    checkForLatest := true,
    validate := {
      val isSbtProject = thisProject.value.base.getName == "project" // TODO find less crude way of doing this

      // Construct a complete module graph, piggy-backing off `sbt-dependency-graph`
      val g: ModuleGraph = GraphOps.pruneEvicted((moduleGraph in Compile).value)
      // Remove the '_2.11' suffixes etc from the artefact names
      val gClean = GraphOps.stripScalaVersionSuffix(g)
      // Retrieve just the resolved module IDs, in topologically sorted order
      val sbtModuleIDs = GraphOps.topoSort(GraphOps.transpose(gClean))

      val localDependencies = libraryDependencies.value
      val transitiveDependendencies = sbtModuleIDs.map(id => ModuleID(id.organisation, id.name, id.version)) //Convert to standard sbt ModuleIDs
      val pluginDependencies = ProjectPlugin.plugins(buildStructure.value)

      Bobby.validateDependencies(
        localDependencies,
        transitiveDependendencies,
        pluginDependencies,
        scalaVersion.value,
        repositories.value,
        checkForLatest.value,
        deprecatedDependenciesUrl.value,
        jsonOutputFileOverride.value,
        isSbtProject
      )
    }
  )
}
