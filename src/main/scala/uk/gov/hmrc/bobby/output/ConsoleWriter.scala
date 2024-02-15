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

package uk.gov.hmrc.bobby.output

import fansi.{Color, Str}
import sbt.ModuleID
import uk.gov.hmrc.bobby.Util._
import uk.gov.hmrc.bobby.domain.BobbyValidationResult

class ConsoleWriter(colours: Boolean) extends TextWriter {

  override def write(bobbyValidationResult: BobbyValidationResult, viewType: ViewType): Unit = {
    logger.info(
       "[bobby] For more information and documentation about Bobby, see the README at https://github.com/hmrc/sbt-bobby")

    logger.info(key().mkString("\n"))

    bobbyValidationResult.allMessages.groupBy(_.scope).foreach { case (scope, messages) =>
      logger.info(s"\nScope: $scope")
      logger.info(renderText(BobbyValidationResult(messages), viewType))
    }

    if (bobbyValidationResult.hasExemptions) {
      val unique = bobbyValidationResult.exemptions.map(m => (m.moduleID, m.deprecationReason.getOrElse(""))).distinct
      logger.warn(s"WARNING: Your build has ${unique.size} bobby exemptions(s). You may wish to take action on these in case exemption is removed in the future.")
      outputSummary(unique).foreach(logger.warn(_))
    }

    if (bobbyValidationResult.hasWarnings) {
      val unique = bobbyValidationResult.warnings.map(m => (m.moduleID, m.deprecationReason.getOrElse(""))).distinct
      logger.warn(s"WARNING: Your build has ${unique.size} bobby warning(s). Please take action to fix these before the listed date, or they will " +
        s"become violations that fail your build")
      outputSummary(unique).foreach(logger.warn(_))
    }

    if (bobbyValidationResult.hasViolations) {
      val unique = bobbyValidationResult.violations.map(m => (m.moduleID, m.deprecationReason.getOrElse(""))).distinct
      logger.error(s"ERROR: Whistle blown! Your build has ${unique.size} bobby violation(s) and has been failed! Urgently fix the issues below:")
      outputSummary(unique).foreach(logger.error(_))
    }

    if (bobbyValidationResult.hasNoIssues)
      logger.info(s"Woohoo, your build has no Bobby issues. Have a great day!")
  }

  override def renderText(bobbyValidationResult: BobbyValidationResult, viewType: ViewType): String = {
    val colouredModel = buildModel(bobbyValidationResult.allMessages, viewType)
    val messageModel  = if (colours) colouredModel else colouredModel.map(_.map(_.plainText.fansi))

    Tabulator.format(viewType.headerNames.map(_.fansi) +: messageModel)
  }

  private def outputSummary(moduleIds: List[(ModuleID, String)]): List[String] =
    moduleIds.zipWithIndex.map { case ((moduleID, reason), idx) =>
      s" (${idx+1}) ${moduleID.moduleName} (${moduleID.revision})\n     Reason: $reason"
    }

  private def key(): Seq[Str] = {
    val colourKey =
      if (colours)
        Seq(
          Str("Dependency Colour KEY: "),
          Color.Blue(" * Local Dependency => Highlights dependencies declared locally in your project"),
          Str(" * Transitive Dependency => Dependencies pulled in via your locally declared dependencies")
        )
      else Seq.empty

    val key =
      Seq(
        Str("*" * 120),
        Str("Level KEY: "),
        Color.Red(" * ERROR: Bobby Violations => Your build will forcibly fail if any violations are detected"),
        Color.Yellow(" * WARN: Bobby Warnings => Your build will" +
          " start to fail from the date the rules become enforced"),
        Color.Green(" * INFO: Bobby Ok => No problems with this dependency"),
        Str("")
      ) ++ colourKey ++ Seq(
        Str("*" * 120)
      )

    if (colours) key else key.map(_.plainText.fansi)
  }
}
