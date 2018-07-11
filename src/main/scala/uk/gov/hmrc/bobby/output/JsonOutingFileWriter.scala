/*
 * Copyright 2018 HM Revenue & Customs
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

import java.io.File
import java.nio.file.Files

import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain.Message

class JsonOutingFileWriter(val filepath: String) {

  private val logger = ConsoleLogger()

//  val filepath: String

  def outputMessagesToJsonFile(messages: List[Message]) = {
    logger.debug("[bobby] Output file set to: " + filepath)
    outputToFile(filepath, renderJson(messages))
  }

  def renderJson(messages: List[Message]): String = {

    val outputMessages = messages.map(_.rawJson)

    s"""{
      | "results" : [
      |   ${outputMessages.mkString(", ")}
      | ]
      |}""".stripMargin

  }

  private def outputToFile(filepath: String, jsonString: String) = {
    val file: File = new File(filepath)
    file.getParentFile.mkdirs()
    logger.debug("[bobby] Writing Bobby report to: " + file.getAbsolutePath);

    Files.write(file.toPath, jsonString.getBytes)
  }

}
