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

import org.joda.time.LocalDate
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import play.api.libs.json._
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain.{BobbyRule, BobbyViolation, BobbyWarning, Dependency, Library, VersionRange}
import uk.gov.hmrc.bobby.domain.MessageBuilder._

class JsonFileWriterSpec extends AnyFlatSpec with Matchers {

  "The JSON file output writer" should "format a list of maps describing the errors and warnings" in {
    val jsonOutputFileWriter: JsonFileWriter = new JsonFileWriter(Configuration.defaultJsonOutputFile)

    val rule = BobbyRule(Dependency("uk.gov.hmrc", "auth"), VersionRange("(,3.0.0]"), "bad library", new LocalDate("2020-01-31") , Library)
    val messages = List(makeMessage(BobbyViolation(rule)), makeMessage(BobbyWarning(rule)))

    val jsonString: String = jsonOutputFileWriter.renderText(messages, Flat)

    val jsValue: JsValue = Json.parse(jsonString)

    val rows: List[JsValue] = (jsValue \ "results").as[List[JsValue]]
    rows.size                          shouldBe 2
    (rows.head \ "level").as[String]   shouldBe "ERROR"
    (rows.head \ "message").as[String] shouldBe "bad library"

    val rowData: JsValue = (rows.head \ "data").as[JsValue]
    (rowData \ "name").as[String]              shouldBe "name"
    (rowData \ "organisation").as[String]      shouldBe "org"
    (rowData \ "revision").as[String]          shouldBe "0.0.0"
    (rowData \ "result").as[String]            shouldBe "BobbyViolation"
    (rowData \ "deprecationFrom").as[String]   shouldBe "2020-01-31" //brexit
    (rowData \ "deprecationReason").as[String] shouldBe "bad library"
    (rowData \ "latestRevision").as[String]    shouldBe "?"

    (rows(1) \ "level").as[String] shouldBe "WARN"
  }

}
