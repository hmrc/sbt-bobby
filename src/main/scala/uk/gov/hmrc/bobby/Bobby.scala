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

import sbt._
import uk.gov.hmrc.bobby.conf.BobbyConfiguration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.Output

class BobbyValidationFailedException(message: String) extends RuntimeException(message)

object Bobby {

  private val logger         = ConsoleLogger()
  private val currentVersion = getClass.getPackage.getImplementationVersion

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
    projectName  : String,
    dependencyMap: Map[ModuleID, Seq[ModuleID]],
    dependencies : Seq[ModuleID],
    config       : BobbyConfiguration
  ): Unit = {

    logger.info(bobbyLogo)

    logger.info(s"[bobby] Bobby version $currentVersion")

    val result =
      BobbyValidator.validate(dependencyMap, dependencies, config.loadBobbyRules(), projectName)

    Output.writeValidationResult(result, config.jsonOutputFile, config.textOutputFile, config.viewType, config.consoleColours)

    if (result.hasViolations)
      throw new BobbyValidationFailedException("Build failed due to bobby violations. See previous output to resolve")

    if (config.strictMode && result.hasWarnings)
      throw new BobbyValidationFailedException("Build failed due to bobby warnings (strict mode is on). See previous output to resolve")
  }
}
