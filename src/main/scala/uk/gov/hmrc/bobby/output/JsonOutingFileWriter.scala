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
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain.DeprecatedDependency

trait JsonOutingFileWriter {

    def outputWarningsToJsonFile(messages: List[(String, String)]) = {
      val json = messages.iterator.map{case (a,b) => (b -> a)}.toMap

      val writer = new PrintWriter(new File("out.json"))
      writer.write(Json.stringify(Json.toJson(json)))
      writer.close()
   }

}

object JsonOutingFileWriter extends JsonOutingFileWriter {
}