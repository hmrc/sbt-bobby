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

package uk.gov.hmrc.bobby

import sbt.Keys._
import sbt._
import uk.gov.hmrc.bobby.conf.{BobbyConfiguration, ConfigFile, ConfigFileImpl}
import uk.gov.hmrc.bobby.output.{Compact, ViewType}
import uk.gov.hmrc.bobby.domain._

object SbtBobbyPlugin extends AutoPlugin {
  import BobbyEnvKeys._
  import BobbyKeys._

  override def trigger = allRequirements

  // Environment variable keys for customising bobby
  object BobbyEnvKeys {
    lazy val envKeyBobbyViewType       = "BOBBY_VIEW_TYPE"
    lazy val envKeyBobbyStrictMode     = "BOBBY_STRICT_MODE"
    lazy val envKeyBobbyConsoleColours = "BOBBY_CONSOLE_COLOURS"
  }

  object BobbyKeys {
    lazy val validateDot             = taskKey[Unit](s"Run Bobby to analyse the dependency dot graphs")
    lazy val bobbyRulesURL           = settingKey[Option[URL]]("Override the URL used to get the list of bobby rules")
    lazy val outputDirectoryOverride = settingKey[Option[String]]("Override the directory used to write the report files")
    lazy val bobbyStrictMode         = settingKey[Boolean]("If true, bobby will fail on warnings as well as violations")
    lazy val bobbyViewType           = settingKey[ViewType]("View type for display: Flat/Nested/Compact")
    lazy val bobbyConsoleColours     = settingKey[Boolean]("If true (default), colours are rendered in the console output")
  }

  private def validateDotTask() =
    Def.task {
      val dir         = target.value
      val projectName = name.value

      val logger      = sLog.value

      // Retrieve config settings
      val bobbyConfigFile: ConfigFile =
        ConfigFileImpl(System.getProperty("user.home") + "/.sbt/bobby.conf", logger)

      val config = new BobbyConfiguration(
          bobbyRulesURL           = bobbyRulesURL.value,
          outputDirectoryOverride = outputDirectoryOverride.value,
          outputFileName          = s"bobby-report-$projectName",
          bobbyConfigFile         = Some(bobbyConfigFile),
          strictMode              = bobbyStrictMode.value,
          viewType                = bobbyViewType.value,
          consoleColours          = bobbyConsoleColours.value
        )

      val bobbyRules = config.loadBobbyRules()

      // Determine nodes to exclude which are this project or dependent projects from this build
      // Required so multi-project builds with modules that depend on each other don't cause a violation of a SNAPSHOT dependency
      val extractedRootProject = Project.extract(state.value)
      val internalModuleNodes =
        buildStructure.value
        .allProjectRefs
        .map(p => extractedRootProject.get(p / projectID))
        .distinct

      Bobby.printHeader(logger)

      object DependencyDotExtractor {
        val DependencyDotRegex = "dependencies-(\\w+).dot".r
        def unapply(file: File): Option[(File, String)] =
          file.getName match {
            case DependencyDotRegex(scope) => Some((file, scope))
            case _                         => None
          }
      }

      // TODO this now analyses all scopes in one pass
      // but what about all multi-modules?
      val dependencyDotFiles =
        // get meta-build files too for plugin scope violations
        (dir.listFiles() ++ new java.io.File("project/target").listFiles()).collect {
          case DependencyDotExtractor(file, scope) => (file, if (file.getPath.contains("project/target")) "plugin" else scope)
        }

      val messages =
        dependencyDotFiles.flatMap { case (file, scope) =>
          logger.info(message(projectName, s"Found $scope ($file)"))
          val content  = IO.read(file)
          val messages = BobbyValidator.validate(content, bobbyRules, internalModuleNodes, projectName)

          // TODO we can combine all the messages together and display as one (e.g. add scopes column)
          uk.gov.hmrc.bobby.output.Output.writeValidationResult(
            BobbyValidationResult(messages.toList),
            config.jsonOutputFile,
            config.textOutputFile,
            config.viewType,
            config.consoleColours
          )

          messages
        }

      val result = BobbyValidationResult(messages.toList)

      if (result.hasViolations)
        throw new uk.gov.hmrc.bobby.BobbyValidationFailedException("Build failed due to bobby violations. See previous output to resolve")

      if (config.strictMode && result.hasWarnings)
        throw new uk.gov.hmrc.bobby.BobbyValidationFailedException("Build failed due to bobby warnings (strict mode is on). See previous output to resolve")
    }

    private val currentVersion =
      getClass.getPackage.getImplementationVersion // This requires that the class is in a package unique to that build

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
  ) ++
  // TODO does this even need to be implemented as a plugin? e.g. just an executable jar which analyses the dot graphs
  // (might work better for multi-module builds)
    addCommandAlias("validateAll", "Compile / dependencyDot; Test / dependencyDot; IntegrationTest / dependencyDot; reload plugins; dependencyDot; reload return; validateDot")
}
