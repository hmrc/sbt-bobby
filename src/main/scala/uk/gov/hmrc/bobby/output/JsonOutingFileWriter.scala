/*
 * Copyright 2015 HM Revenue & Customs
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

import java.io.{File, PrintWriter}

import play.api.libs.json.Json
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.Message
import uk.gov.hmrc.bobby.conf.Configuration

trait JsonOutingFileWriter {

  private val logger = ConsoleLogger()

  val filepath: Option[String]

  def outputMessagesToJsonFile(messages: List[Message]) = {
    logger.info("Output file set to: " + filepath)
    if (!filepath.isEmpty) {
      outputToFile(filepath.get, renderJson(messages))
    }
  }

  def renderJson(messages: List[Message]): String = {

    val outputMessages = messages.map(_.jsonOutput )
    val outputStructure = Map("results" -> outputMessages)
    Json.stringify(Json.toJson(outputStructure))
  }

  private def outputToFile(filepath: String, jsonString: String) = {
    val file: File = new File(filepath)
    logger.info("Outputting results to JSON file: " + file.getAbsolutePath);
    val writer = new PrintWriter(file)
    writer.write(jsonString)
    writer.close()
  }

}

object JsonOutingFileWriter extends JsonOutingFileWriter {

  override val filepath: Option[String] = Configuration.outputFile

}