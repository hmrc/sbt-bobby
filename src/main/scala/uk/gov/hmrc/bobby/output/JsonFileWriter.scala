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

import uk.gov.hmrc.bobby.domain.{BobbyOk, BobbyViolation, BobbyWarning, Message}

class JsonFileWriter(val filepath: String) extends BobbyWriter with FileWriter {

  override def renderText(messages: List[Message], viewType: ViewType): String = {

    def rawJson(m: Message): String =
      s"""{ "level" : "${m.level.name}",
         |  "message" : "${jsonMessage(m)}",
         |  "data": {
         |    "organisation" : "${m.checked.moduleID.organization}",
         |    "name" : "${m.checked.moduleID.name}",
         |    "revision" : "${m.checked.moduleID.revision}",
         |    "result" : "${m.checked.result.getClass.getSimpleName}",
         |    "deprecationFrom" : "${m.deprecationFrom.getOrElse("-")}",
         |    "deprecationReason" : "${m.deprecationReason.getOrElse("-")}",
         |    "latestRevision" : "${m.latestVersion.getOrElse("?")}"
         |  }
         |}""".stripMargin

    val outputMessages = messages.map(rawJson)

    s"""{
       | "results" : [
       |   ${outputMessages.mkString(", ")}
       | ]
       |}""".stripMargin
  }

  def jsonMessage(m: Message): String = m.checked.result match {
    case BobbyOk => ""
    case BobbyWarning(r) =>
      s"${m.checked.moduleID.organization}.${m.checked.moduleID.name} ${m.checked.moduleID.revision} is deprecated: '${r.reason}'. " +
        s"To be updated by ${r.effectiveDate} to version ${m.latestVersion.getOrElse("?")}"
    case BobbyViolation(r) => r.reason
  }

}
