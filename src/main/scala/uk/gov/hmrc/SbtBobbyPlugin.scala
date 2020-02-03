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

import net.virtualvoid.sbt.graph.{ModuleGraph, ModuleId}
import sbt.Keys._
import sbt._
import uk.gov.hmrc.bobby.output.{Compact, ViewType}
import uk.gov.hmrc.bobby.{Bobby, GraphOps, ProjectPlugin}

object SbtBobbyPlugin extends AutoPlugin {

  override def trigger = allRequirements

  private val ENV_KEY_BOBBY_VIEW_TYPE        = "BOBBY_VIEW_TYPE"

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

    val bobbyViewType = settingKey[ViewType]("View type for display: Flat/Nested/Compact")

  }

  import BobbyKeys._
  import net.virtualvoid.sbt.graph.DependencyGraphKeys._
  import uk.gov.hmrc.bobby.Util._

  override lazy val projectSettings = Seq(
    deprecatedDependenciesUrl := None,
    jsonOutputFileOverride := None,
    parallelExecution in GlobalScope := true,
    repositories := Seq(Artifactory, Bintray),
    checkForLatest := true,
    bobbyViewType := sys.env.get(ENV_KEY_BOBBY_VIEW_TYPE).map(ViewType.apply).getOrElse(Compact),
    validate := {
      // Construct a complete module graph of the project (not plugin) dependencies, piggy-backing off `sbt-dependency-graph`
      val projectDependencyGraph: ModuleGraph = GraphOps.cleanGraph((moduleGraph in Compile).value, ModuleId(organization.value.trim, name.value.trim, version.value.trim))

      // Retrieve the plugin dependencies. It would be nice to generate these in the same way via the full ModuleGraph, however the
      // sbt UpdateReport in the pluginData is not rich enough. Seems we have the nodes but not the edges.
      // Instead we just extract the locally added plugins
      val pluginDependencies = ProjectPlugin.plugins(buildStructure.value).toSet.toList

      // Retrieve just the resolved module IDs, in topologically sorted order
      val projectDependencies = GraphOps.topoSort(GraphOps.transpose(projectDependencyGraph))

      // Construct a dependency map from each ModuleId -> Sequential list of transitive ModuleIds that brought it in, the tail being the origin
      val dependencyMap = GraphOps.reverseDependencyMap(projectDependencyGraph, projectDependencies)

      Bobby.validateDependencies(
        GraphOps.toSbtDependencyMap(dependencyMap), //Use vanilla sbt ModuleIDs
        projectDependencies.map(_.toSbt),           //Use vanilla sbt ModuleIDs
        pluginDependencies,
        scalaVersion.value,
        repositories.value,
        checkForLatest.value,
        bobbyViewType.value,
        deprecatedDependenciesUrl.value,
        jsonOutputFileOverride.value
      )
    }
  )
}
