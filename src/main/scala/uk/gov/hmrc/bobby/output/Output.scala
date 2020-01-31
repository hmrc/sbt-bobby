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
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain.MessageLevels.ERROR
import uk.gov.hmrc.bobby.domain._

object Output {

  val logger = ConsoleLogger()

  def writeMessagesToFile(messages: List[Message], jsonFilePath: String, textFilePath: String): Unit = {
    new JsonOutingFileWriter(jsonFilePath).outputMessagesToJsonFile(messages)
    new TextOutingFileWriter(textFilePath).outputMessagesToTextFile(messages)
  }

  def outputMessagesToConsole(messages: List[Message]): Unit = {
    val model = buildTabularOutputModel(messages)

    logger.info(
      "[bobby] Bobby info and warnings. See bobby report artefact for more details. For more information " +
        "and documentation regarding bobby, please see the README at https://github.com/hmrc/sbt-bobby")

    Tabulator.formatAsStrings(Message.shortTabularHeader.map(s => fansi.Str(s)) +: model).foreach { log =>
      logger.info(log)
    }

    messages.filter(_.isError).foreach { log =>
      renderConsoleErrorMessage(log.jsonMessage)
    }
  }

  implicit class FansiMessage(m: Message) {
    def fansi: Seq[Str] = {
      Seq(
        if(m.level == ERROR) Color.Red(m.level.name) else Color.Cyan(m.level.name),
        if(m.dependencyChain.isEmpty) Color.Yellow(m.moduleName) else Str(m.moduleName),
        Str(m.dependencyChain.lastOption.map(m.buildModuleName).getOrElse("")),
        Str(m.module.revision),
        Str(m.result.rule.map(_.range.toString()).getOrElse("-")),
        Str(m.latestVersion.map(_.toString).getOrElse("?")),
        Str(m.deadline.map(_.toString).getOrElse("-"))
      )
    }
  }

  def buildTabularOutputModel(messages: List[Message]): List[Seq[fansi.Str]] =
    messages
      .sortBy(_.moduleName)
      .sortWith((a, b) => MessageLevels.compare(a.level, b.level))
      .map { m =>
        m.fansi
      }

  def renderConsoleErrorMessage(text: String): Unit = {
    logger.error("-")
    logger.error("- Bobby mandatory failure details:")
    logger.error("-")
    logger.error(text)
    logger.error("")
  }
}
