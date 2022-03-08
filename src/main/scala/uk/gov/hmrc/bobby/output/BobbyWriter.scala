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

package uk.gov.hmrc.bobby.output

import java.io.File
import java.nio.file.Files
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain.BobbyValidationResult

trait BobbyWriter {

  val logger = ConsoleLogger()

  def write(bobbyValidationResult: BobbyValidationResult, viewType: ViewType): Unit

  def renderText(bobbyValidationResult: BobbyValidationResult, viewType: ViewType): String

}

trait FileWriter extends BobbyWriter {

  val filepath: String

  def write(bobbyValidationResult: BobbyValidationResult, viewType: ViewType) {
    val file: File = new File(filepath)
    file.getParentFile.mkdirs()
    logger.info(s"[bobby] ${getClass.getSimpleName} - Writing Bobby report to: " + file.getAbsolutePath)

    Files.write(file.toPath, renderText(bobbyValidationResult, viewType).getBytes)
  }

}
