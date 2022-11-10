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

import uk.gov.hmrc.bobby.domain.{Message, MessageLevels}
import uk.gov.hmrc.bobby.Util._

trait TextWriter extends BobbyWriter {

  def buildModel(messages: List[Message], viewType: ViewType): List[Seq[fansi.Str]] =
    viewType match {
      case Nested =>
        val transitiveMessages = messages.filterNot(_.isLocal).groupBy(m => m.dependencyChain.lastOption)
          .collect {
            case (Some(k), v) => k -> v
          }

        val groupedMessages = messages
          .filter(_.isLocal).flatMap { m =>
          List(m) ++ transitiveMessages.getOrElse(m.moduleID, List.empty)
        }
        groupedMessages.map(viewType.renderMessage)

      case _ =>
        messages
          .sortBy(_.moduleID.moduleName)
          .sortWith((a, b) => MessageLevels.compare(a.level, b.level))
          .map(viewType.renderMessage)
    }
}
