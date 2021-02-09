/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}

object Message {

  implicit object MessageOrdering extends Ordering[Message] {
    def compare(a: Message, b: Message): Int = a.level compare b.level
  }

}

case class Message(
  checked: BobbyChecked,
  dependencyChain: Seq[ModuleID]) {

  val level: MessageLevels.Level = checked.result match {
    case BobbyOk            => INFO
    case BobbyWarning(_)    => WARN
    case BobbyViolation(_)  => ERROR
  }

  val deprecationReason: Option[String]    = checked.result.rule.map(_.reason)
  val deprecationFrom: Option[LocalDate]   = checked.result.rule.map(_.effectiveDate)
  val effectiveDate: Option[LocalDate]     =  checked.result.rule.map(_.effectiveDate)
  val moduleName: String                   = checked.moduleID.moduleName

  val isError: Boolean = level.equals(MessageLevels.ERROR)
  val isWarning: Boolean = level.equals(MessageLevels.WARN)
  val isOkay: Boolean = level.equals(MessageLevels.INFO)

  val isLocal: Boolean = dependencyChain.isEmpty

}
