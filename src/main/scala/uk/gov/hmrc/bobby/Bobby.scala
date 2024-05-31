/*
 * Copyright 2024 HM Revenue & Customs
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

import sbt.ModuleID
import sbt.util.Logger
import uk.gov.hmrc.bobby.conf.BobbyConfiguration
import uk.gov.hmrc.bobby.domain.{BobbyValidator, BobbyValidationResult}
import uk.gov.hmrc.bobby.output.{ConsoleWriter, JsonFileWriter, TextFileWriter}

class BobbyValidationFailedException(message: String) extends RuntimeException(message)

object Bobby {

  private val currentVersion =
    getClass.getPackage.getImplementationVersion

  private val bobbyLogo =
    """
      |              ,
      |     __  _.-"` `'-.
      |    /||\'._ __{}_(
      |    ||||  |'--.__\           SBT BOBBY
      |    |  L.(   ^_\^    Your friendly neighbourhood
      |    \ .-' |   _ |          build policeman
      |    | |   )\___/
      |    |  \-'`:._]
      |     \__/;      '-.
      |""".stripMargin

  def validateDependencies(
    rootName           : String,
    projectName        : String,
    dependencyDotFiles : Seq[DotFile],
    internalModuleNodes: Seq[ModuleID],
    config             : BobbyConfiguration,
    logger             : Logger
  ): Unit = {
    logger.info(bobbyLogo)

    logger.info(s"[bobby] Bobby version $currentVersion")

    val bobbyRules = config.loadBobbyRules()

    val messages =
      dependencyDotFiles.flatMap { dotFile =>
        val messages = BobbyValidator.validate(dotFile.content, dotFile.scope, bobbyRules, internalModuleNodes, rootName)

        val outputFileName = s"bobby-report-$projectName-${dotFile.scope}"

        new JsonFileWriter(s"${config.outputDirectory}/${outputFileName}.json")
          .write(BobbyValidationResult(messages), config.viewType)

        new TextFileWriter(s"${config.outputDirectory}/${outputFileName}.txt")
          .write(BobbyValidationResult(messages), config.viewType)

        messages
      }

    val result = BobbyValidationResult(messages)

    new ConsoleWriter(config.consoleColours).write(result, config.viewType)

    if (result.hasViolations)
      throw new BobbyValidationFailedException("Build failed due to bobby violations. See previous output to resolve")

    if (config.strictMode && result.hasWarnings)
      throw new BobbyValidationFailedException("Build failed due to bobby warnings (strict mode is on). See previous output to resolve")
  }

  case class DotFile(
    name   : String,
    content: String,
    scope  : String
  )
}
