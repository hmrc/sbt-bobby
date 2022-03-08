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
import uk.gov.hmrc.bobby.Util._
import uk.gov.hmrc.bobby.domain.BobbyValidationResult

class TextFileWriter(val filepath: String) extends TextWriter with FileWriter {

  override def renderText(bobbyValidationResult: BobbyValidationResult, viewType: ViewType): String = {
    val messageModel = buildModel(bobbyValidationResult.allMessages, viewType).map(_.map(_.plainText.fansi))

    Tabulator.format(viewType.headerNames.map(_.fansi) +: messageModel)
  }

}
