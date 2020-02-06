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

package uk.gov.hmrc.bobby.output

import fansi.{Color, Str}
import uk.gov.hmrc.bobby.Util._
import uk.gov.hmrc.bobby.domain.Message

class ConsoleWriter(colours: Boolean) extends TextWriter {

  override def write(messages: List[Message], viewType: ViewType): Unit = {
    logger.info(
       "[bobby] For more information and documentation about Bobby, see the README at https://github.com/hmrc/sbt-bobby")

    logger.info(key().mkString("\n"))

    val needsAttention = messages.filterNot(_.isOkay)

    logger.info(renderText(messages, viewType))

    if(needsAttention.nonEmpty){
      val (violations, warnings) = needsAttention.partition(_.isError)

      if(warnings.nonEmpty){
        logger.warn(s"WARNING: Your build has ${warnings.size} bobby warning(s). Please take action to fix these before the listed date, or they will " +
          s"become violations that fail your build")
        outputSummary(warnings).foreach(logger.warn(_))
      }

      if(violations.nonEmpty){
        logger.error(s"ERROR: Whistle blown! Your build has ${violations.size} bobby violation(s) and has been failed! Urgently fix the issues below:")
        outputSummary(violations).foreach(logger.error(_))
      }
    } else {
      logger.info(s"Woohoo, your build has no Bobby issues. Have a great day!")
    }

  }

  override def renderText(messages: List[Message], viewType: ViewType): String = {
    val colouredModel = buildModel(messages, viewType)

    val messageModel = if(colours) colouredModel else colouredModel.map(_.map(_.plainText.fansi))

    Tabulator.format(viewType.headerNames.map(_.fansi) +: messageModel)
  }

  private def outputSummary(messages: List[Message]): List[String] =
        messages.zipWithIndex.map{ case (m, idx) => s" (${idx+1}) ${m.moduleName} (${m.checked.moduleID.revision})\n     Reason: ${m.deprecationReason.getOrElse("")}" }

  private def key(): Seq[Str] = {
    val key = Seq(
      Str("*" * 120),
      Str("Level KEY: "),
      Color.Red(" * ERROR: Bobby Violations => Your build will forcibly fail if any violations are detected"),
      Color.Yellow(" * WARN: Bobby Warnings => Your build will" +
        " start to fail from the date the rules become enforced"),
      Color.Green(" * INFO: Bobby Ok => No problems with this dependency"),
      Str(""),
      Str("Dependency KEY: "),
      Color.Blue(" * L: Local Dependency => Highlights dependencies declared locally in your project (not transitive)"),
      Str(" * T: Transitive Dependency => Dependencies pulled in via your locally declared dependencies"),
      Color.Magenta(" * P: Plugin Dependency => From your build project"),
      Str("*" * 120)
    )
    if(colours) key else key.map(_.plainText.fansi)
  }

}

