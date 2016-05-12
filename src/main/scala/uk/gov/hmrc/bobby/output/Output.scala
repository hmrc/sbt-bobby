/*
 * Copyright 2016 HM Revenue & Customs
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

import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain._

object Output {

  val logger = ConsoleLogger()

  def outputMessages(messages: List[Message], jsonFilePath: String, textFilePath: String): Unit = {

    outputMessagesToConsole(messages)
    new JsonOutingFileWriter(jsonFilePath).outputMessagesToJsonFile(messages)
    new TextOutingFileWriter(textFilePath).outputMessagesToTextFile(messages)
  }

  private def outputMessagesToConsole(messages: List[Message]): Unit = {
    val model = buildTabularOutputModel(messages)

    logger.info("[bobby] Bobby info and warnings. See bobby report artefact for more details. For more information " +
      "and documentation regarding bobby, please see the README at https://github.com/hmrc/sbt-bobby")

    Tabulator.formatAsStrings(Message.shortTabularHeader +: model).foreach { log =>
      logger.info(log)
    }

    messages.filter(_.isError).foreach { log =>
      renderConsoleErrorMessage(log.jsonMessage)
    }
  }

  def buildTabularOutputModel(messages: List[Message]): List[Seq[String]] = {
    messages
      .sortBy(_.moduleName)
      .sortWith((a, b) => MessageLevels.compare(a.level, b.level))
      .map { m => m.shortTabularOutput }
  }

  def renderConsoleErrorMessage(text: String): Unit = {
    logger.error("-")
    logger.error("- Bobby mandatory failure details:")
    logger.error("-")
    logger.error(text)
    logger.error("")
  }
}
