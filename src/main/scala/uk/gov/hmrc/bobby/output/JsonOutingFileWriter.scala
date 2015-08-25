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

trait JsonOutingFileWriter {

    def outputWarningsToJsonFile(messages: List[(String, String)]) = {
      val jsonString: String = renderJson(messages)

      val writer = new PrintWriter(new File("out.json"))
      writer.write(jsonString)
      writer.close()
   }

    def renderJson(messages: List[(String, String)]): String = {
      val outputMessages: List[Map[String, String]] = messages.map(row => {
        Map("message" -> row._1, "level" -> row._2)
      })

      val outputStructure: Map[String, List[Map[String, String]]] = Map("results" -> outputMessages)

      Json.stringify(Json.toJson(outputStructure))
    }

}

object JsonOutingFileWriter extends JsonOutingFileWriter {
}