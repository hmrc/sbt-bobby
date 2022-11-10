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

package uk.gov.hmrc.bobby.domain

import java.time.LocalDate

import sbt.ModuleID
import uk.gov.hmrc.bobby.Util._

object Message {

  implicit val ordering: Ordering[Message] =
    Ordering.by(_.level)
}

case class Message(
  // TODO restore BobbyChecked? Since the BobbyChecked is the same, but different dependencyChain per scope
  moduleID       : ModuleID,
  result         : BobbyResult,
  scope          : String,
  dependencyChain: Seq[ModuleID]
) {

  val level: MessageLevels.Level =
    result match {
      case BobbyResult.Ok            => MessageLevels.INFO
      case BobbyResult.Exemption(_)  => MessageLevels.WARN
      case BobbyResult.Warning(_)    => MessageLevels.WARN
      case BobbyResult.Violation(_)  => MessageLevels.ERROR
    }

  val deprecationReason: Option[String] =
    result.rule.map(_.reason)

  val effectiveDate: Option[LocalDate] =
    result.rule.map(_.effectiveDate)

  val isLocal: Boolean =
    dependencyChain.isEmpty

  def pulledInBy: Option[String] =
    dependencyChain.lastOption.map(_.moduleName)
}
