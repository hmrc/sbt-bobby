/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.bobby.domain.{BobbyResult, BobbyValidationResult, Message}

class JsonFileWriter(val filepath: String) extends BobbyWriter with FileWriter {

  override def renderText(bobbyValidationResult: BobbyValidationResult, viewType: ViewType): String = {

    val json: JsValue = Json.obj(
      "results" -> bobbyValidationResult.allMessages.map { m =>
        Json.obj(
          "level"   -> m.level.name,
          "message" -> jsonMessage(m),
          "data"    -> Json.obj(
            "organisation"      -> m.moduleID.organization,
            "name"              -> m.moduleID.name,
            "revision"          -> m.moduleID.revision,
            "result"            -> m.result.name,
            "deprecationFrom"   -> JsString(m.effectiveDate.map(_.toString).getOrElse("-")),
            "deprecationReason" -> JsString(m.deprecationReason.getOrElse("-"))
          )
        )
      }
    )

    Json.prettyPrint(json)
  }

  def jsonMessage(m: Message): String =
    m.result match {
      case BobbyResult.Ok           => "No issue"
      case BobbyResult.Exemption(_) => "Exemption applies; may need attention"
      case BobbyResult.Warning(_)   => "Needs attention soon to avoid future violations"
      case BobbyResult.Violation(_) => "Needs urgent attention - preventing build"
    }
}
