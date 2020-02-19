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
import uk.gov.hmrc.bobby.conf.ConfigFile
import uk.gov.hmrc.bobby.output.{Compact, ViewType}
import uk.gov.hmrc.bobby.{Bobby, GraphOps, PluginDependencyResolver}

object SbtBobbyPlugin extends AutoPlugin {

  override def trigger = allRequirements

  // Environment variable keys for customising bobby
  object BobbyEnvKeys {
    lazy val envKeyBobbyViewType = "BOBBY_VIEW_TYPE"
    lazy val envKeyBobbyStrictMode = "BOBBY_STRICT_MODE"
    lazy val envKeyBobbyConsoleColours = "BOBBY_CONSOLE_COLOURS"
  }

  object BobbyKeys {

    lazy val validate     = TaskKey[Unit]("validate", "Run Bobby to validate dependencies")
    lazy val deprecatedDependenciesUrl =
      SettingKey[Option[URL]]("dependencyUrl", "Override the URL used to get the list of deprecated dependencies")
    lazy val jsonOutputFileOverride =
      SettingKey[Option[String]]("jsonOutputFileOverride", "Override the file used to write json result file")
    lazy val bobbyStrictMode = settingKey[Boolean]("If true, bobby will fail on warnings as well as violations")
    lazy val bobbyViewType = settingKey[ViewType]("View type for display: Flat/Nested/Compact")
    lazy val bobbyConsoleColours = settingKey[Boolean]("If true (default), colours are rendered in the console output")

  }

  import BobbyEnvKeys._
  import BobbyKeys._
  import net.virtualvoid.sbt.graph.DependencyGraphKeys._
  import uk.gov.hmrc.bobby.Util._

  override lazy val projectSettings = Seq(
    deprecatedDependenciesUrl := None,
    jsonOutputFileOverride := None,
    parallelExecution in GlobalScope := true,
    bobbyViewType := sys.env.get(envKeyBobbyViewType).map(ViewType.apply).getOrElse(Compact),
    bobbyStrictMode := sys.env.get(envKeyBobbyStrictMode).map(_.toBoolean).getOrElse(false),
    bobbyConsoleColours := sys.env.get(envKeyBobbyConsoleColours).map(_.toBoolean).getOrElse(true),
    validate := {
      // Determine nodes to exclude which are this project or dependent projects from this build
      // Required so multi-project builds with modules that depend on each other don't cause a violation of a SNAPSHOT dependency
      val extracted = Project.extract(state.value)
      val internalModuleNodes = buildStructure.value.allProjectRefs.map( p =>
        extracted.get(projectID in p)
      ).distinct.map(_.toDependencyGraph)

      // Construct a complete module graph of the project (not plugin) dependencies, piggy-backing off `sbt-dependency-graph`
      val projectDependencyGraph: ModuleGraph = GraphOps.cleanGraph((moduleGraph in Compile).value, excludeNodes = internalModuleNodes)

      // Retrieve the plugin dependencies. It would be nice to generate these in the same way via the full ModuleGraph, however the
      // sbt UpdateReport in the pluginData is not rich enough. Seems we have the nodes but not the edges.
      val pluginDependencies = PluginDependencyResolver.plugins(buildStructure.value).distinct

      // Retrieve just the resolved module IDs, in topologically sorted order
      val projectDependencies = GraphOps.topoSort(GraphOps.transpose(projectDependencyGraph))

      // Construct a dependency map from each ModuleId -> Sequential list of transitive ModuleIds that brought it in, the tail being the origin
      val dependencyMap = GraphOps.reverseDependencyMap(projectDependencyGraph, projectDependencies)

      // Retrieve config settings
      val bobbyConfigFile: ConfigFile = new ConfigFile(System.getProperty("user.home") + "/.sbt/bobby.conf")

      Bobby.validateDependencies(
        bobbyStrictMode.value,
        GraphOps.toSbtDependencyMap(dependencyMap), //Use vanilla sbt ModuleIDs
        projectDependencies.map(_.toSbt),           //Use vanilla sbt ModuleIDs
        pluginDependencies,
        scalaVersion.value,
        bobbyViewType.value,
        bobbyConsoleColours.value,
        deprecatedDependenciesUrl.value,
        Some(bobbyConfigFile),
        jsonOutputFileOverride.value
      )
    }
  )
}
