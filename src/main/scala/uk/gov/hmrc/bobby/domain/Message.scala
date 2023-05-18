/*
 * Copyright 2023 HM Revenue & Customs
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
  moduleID       : ModuleID,
  result         : BobbyResult,
  scope          : String,
  dependencyChain: Seq[ModuleID]
) {

  val level: MessageLevel =
    result match {
      case BobbyResult.Ok            => MessageLevel.INFO
      case BobbyResult.Exemption(_)  => MessageLevel.WARN
      case BobbyResult.Warning(_)    => MessageLevel.WARN
      case BobbyResult.Violation(_)  => MessageLevel.ERROR
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

sealed abstract class MessageLevel(val order: Int, val name: String) extends Ordered[MessageLevel] {
  def compare(that: MessageLevel): Int =
    this.order - that.order

  override def toString = name
}

object MessageLevel {
  case object ERROR extends MessageLevel(0, "ERROR")
  case object WARN extends  MessageLevel(1, "WARN")
  case object INFO extends  MessageLevel(2, "INFO")
}
