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

import org.scalatest.{Matchers, FlatSpec}
import play.api.libs.json._
import sbt.ModuleID
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain.{MessageLevels, MessageBuilder, Version}
import MessageLevels._
import MessageBuilder._



class JsonOutputFileWriterSpec extends FlatSpec with Matchers {

    "The JSON file output writer" should "format a list of maps describing the errors and warnings" in {
      val jsonOutputFileWriter: JsonOutingFileWriter = new JsonOutingFileWriter(Configuration.defaultJsonOutputFile)
      val messages = List(makeMessage(ERROR, "An error"), makeMessage(WARN, "Another warning"))
      val jsonString: String = jsonOutputFileWriter.renderJson(messages)

      val jsValue: JsValue = Json.parse(jsonString)

      val rows: List[JsValue] = (jsValue \ "results").as[List[JsValue]]
      rows.size shouldBe 2
      (rows.head \ "level").as[String] shouldBe "ERROR"
      (rows.head \ "message").as[String] shouldBe "An error"
      (rows(1) \ "level").as[String] shouldBe "WARN"
    }

}
