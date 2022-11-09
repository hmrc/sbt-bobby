/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc // TODO move to bobby package

import net.virtualvoid.sbt.graph.ModuleGraph
import sbt.Keys._
import sbt._
import uk.gov.hmrc.bobby.conf.{BobbyConfiguration, ConfigFile, ConfigFileImpl}
import uk.gov.hmrc.bobby.output.{Compact, ViewType}
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.{Bobby, GraphOps}
import uk.gov.hmrc.graph.DependencyGraphParser

object SbtBobbyPlugin extends AutoPlugin {

  override def trigger = allRequirements

  import net.virtualvoid.sbt.graph.DependencyGraphKeys._
  import BobbyEnvKeys._
  import BobbyKeys._
  import uk.gov.hmrc.bobby.Util._

  // Environment variable keys for customising bobby
  object BobbyEnvKeys {
    lazy val envKeyBobbyViewType       = "BOBBY_VIEW_TYPE"
    lazy val envKeyBobbyStrictMode     = "BOBBY_STRICT_MODE"
    lazy val envKeyBobbyConsoleColours = "BOBBY_CONSOLE_COLOURS"
  }

  object BobbyKeys {
    lazy val validate                = taskKey[Unit]("Run Bobby to validate dependencies")
    lazy val bobbyRulesURL           = settingKey[Option[URL]]("Override the URL used to get the list of bobby rules")
    lazy val outputDirectoryOverride = settingKey[Option[String]]("Override the directory used to write the report files")
    lazy val bobbyStrictMode         = settingKey[Boolean]("If true, bobby will fail on warnings as well as violations")
    lazy val bobbyViewType           = settingKey[ViewType]("View type for display: Flat/Nested/Compact")
    lazy val bobbyConsoleColours     = settingKey[Boolean]("If true (default), colours are rendered in the console output")
    lazy val validateDot = // or replace validate?
      taskKey[Unit](s"Run Bobby to analyse the dependency dot graphs")
  }

  def validateSettings: Seq[Def.Setting[_]] =
    Seq(Compile, Test, IntegrationTest, Runtime, Provided, Optional)
      .flatMap(validateTaskForConfig)

  // Define the validate task for the given configuration
  def validateTaskForConfig(config: Configuration): Seq[Def.Setting[_]] = inConfig(config)(
    Seq(
      validate := {
        // Determine nodes to exclude which are this project or dependent projects from this build
        // Required so multi-project builds with modules that depend on each other don't cause a violation of a SNAPSHOT dependency
        val extractedRootProject = Project.extract(state.value)
        val internalModuleNodes =
          buildStructure.value
          .allProjectRefs
          .map(p => extractedRootProject.get(p / projectID))
          .distinct
          .map(_.toDependencyGraph)

        // Construct a complete module graph piggy-backing off `sbt-dependency-graph`
        val projectDependencyGraph: ModuleGraph = GraphOps.cleanGraph((config / moduleGraph).value, excludeNodes = internalModuleNodes)

        // Retrieve just the resolved module IDs, in topologically sorted order
        val projectDependencies = GraphOps.topoSort(GraphOps.transpose(projectDependencyGraph))

        // Construct a dependency map from each ModuleId -> Sequential list of transitive ModuleIds that brought it in, the tail being the origin
        val dependencyMap = GraphOps.reverseDependencyMap(projectDependencyGraph, projectDependencies)

        // Retrieve config settings
        val bobbyConfigFile: ConfigFile = ConfigFileImpl(System.getProperty("user.home") + "/.sbt/bobby.conf")

        val bobbyConfig = new BobbyConfiguration(
          bobbyRulesURL           = bobbyRulesURL.value,
          outputDirectoryOverride = outputDirectoryOverride.value,
          outputFileName          = s"bobby-report-${thisProject.value.id}-${config.name}",
          bobbyConfigFile         = Some(bobbyConfigFile),
          strictMode              = bobbyStrictMode.value,
          viewType                = bobbyViewType.value,
          consoleColours          = bobbyConsoleColours.value
        )

        val projectName = name.value

        Bobby.validateDependencies(
          projectName,
          GraphOps.toSbtDependencyMap(dependencyMap), //Use vanilla sbt ModuleIDs
          projectDependencies.map(_.toSbt),           //Use vanilla sbt ModuleIDs
          bobbyConfig
        )
      }
    )
  )

    private def validateDotTask() =
    Def.task {
      val dependencyGraphParser = new DependencyGraphParser
      val dir = target.value
      val projectName = name.value


      // Retrieve config settings
      val bobbyConfigFile: ConfigFile = ConfigFileImpl(System.getProperty("user.home") + "/.sbt/bobby.conf")

      val outputFileName = s"bobby-report" //s"bobby-report-${thisProject.value.id}-${config.name}"

      val config = new BobbyConfiguration(
          bobbyRulesURL           = bobbyRulesURL.value,
          outputDirectoryOverride = outputDirectoryOverride.value,
          outputFileName          = outputFileName,
          bobbyConfigFile         = Some(bobbyConfigFile),
          strictMode              = bobbyStrictMode.value,
          viewType                = bobbyViewType.value,
          consoleColours          = bobbyConsoleColours.value
        )

      val bobbyRules = config.loadBobbyRules()

      val today = java.time.LocalDate.now()

      // Determine nodes to exclude which are this project or dependent projects from this build
      // Required so multi-project builds with modules that depend on each other don't cause a violation of a SNAPSHOT dependency
      val extractedRootProject = Project.extract(state.value)
      val internalModuleNodes =
        buildStructure.value
        .allProjectRefs
        .map(p => extractedRootProject.get(p / projectID))
        .distinct

      dir.listFiles().map { file =>
        if (file.getName.startsWith("dependencies") && file.getName.endsWith(".dot")) { // TODO regex

          //what about plugin scope?
          //"project/target/dependencies-compile.dot"
          println(s"Found $file")
          val content = IO.read(file)
          val graph = dependencyGraphParser.parse(content)
          val dependencies =
            graph.dependencies.filterNot { n1 =>
              internalModuleNodes.exists(n2 => n1.group == n2.organization && n1.artefact == n2.name)
            }


          val messages =
            dependencies.map { dependency =>

              val matchingRules =
                bobbyRules
                  .filter { rule =>
                    (rule.dependency.organisation.equals(dependency.group) || rule.dependency.organisation.equals("*")) &&
                      (rule.dependency.name.equals(dependency.artefactWithoutScalaVersion) || rule.dependency.name.equals("*")) &&
                      rule.range.includes(Version(dependency.version))
                  }
                  .sorted

              val result =
                matchingRules
                .map { rule =>
                  if (rule.exemptProjects.contains(projectName))
                    BobbyExemption(rule): BobbyResult
                  else if (rule.effectiveDate.isBefore(today) || rule.effectiveDate.isEqual(today))
                    BobbyViolation(rule)
                  else
                    BobbyWarning(rule)
                }
                .sorted
                .headOption
                .getOrElse(BobbyOk)


              Message(
                checked         = BobbyChecked(
                                    moduleID = ModuleID(dependency.group, dependency.artefact, dependency.version),
                                    result   = result
                                  ),
                dependencyChain = graph.pathToRoot(dependency).map(d => ModuleID(d.group, d.artefact, d.version)).dropRight(1) // last one is the project itself
              )
            }

          val result = BobbyValidationResult(messages.toList)

          uk.gov.hmrc.bobby.output.Output.writeValidationResult(result, config.jsonOutputFile, config.textOutputFile, config.viewType, config.consoleColours)

          if (result.hasViolations)
            throw new uk.gov.hmrc.bobby.BobbyValidationFailedException("Build failed due to bobby violations. See previous output to resolve")

          if (config.strictMode && result.hasWarnings)
            throw new uk.gov.hmrc.bobby.BobbyValidationFailedException("Build failed due to bobby warnings (strict mode is on). See previous output to resolve")
        }
      }
    }

    private val currentVersion =
      getClass.getPackage.getImplementationVersion // This requires that the class is in a package unique to that build (not currently true!)

    private def message(projectName: String, msg: String): String =
      s"SbtBobby [$currentVersion] ($projectName) - $msg"


  override lazy val projectSettings = Seq(
    bobbyRulesURL                   := None,
    outputDirectoryOverride         := None,
    GlobalScope / parallelExecution := true,
    bobbyViewType       := sys.env.get(envKeyBobbyViewType).map(ViewType.apply).getOrElse(Compact),
    bobbyStrictMode     := sys.env.get(envKeyBobbyStrictMode).map(_.toBoolean).getOrElse(false),
    bobbyConsoleColours := sys.env.get(envKeyBobbyConsoleColours).map(_.toBoolean).getOrElse(true),
    validateDot         := validateDotTask().value
  ) ++ validateSettings ++
    //Add a useful alias to run bobby validate for compile, test and plugins together (similar to old releases of bobby)
    //addCommandAlias("validateAll", ";Compile / validate; Test / validate; IntegrationTest / validate; reload plugins; validate; reload return")
    // TODO the dependencyDot on build server may have already been extracted.
    addCommandAlias("validateAll", ";Compile / dependencyDot; Test / dependencyDot; IntegrationTest / dependencyDot; reload plugins; dependencyDot; reload return; validateDot")
}
