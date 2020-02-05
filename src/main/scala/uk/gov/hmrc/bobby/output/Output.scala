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

import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain._

object Output {

  val logger = ConsoleLogger()

  def writeMessages(messages: List[Message], jsonFilePath: String, textFilePath: String, viewType: ViewType, consoleColours: Boolean): Unit = {

    val writers = Seq(
      new JsonFileWriter(jsonFilePath),
      new TextFileWriter(textFilePath),
      new ConsoleWriter(consoleColours)
    )

    writers.foreach(_.write(messages, viewType))
  }

}
