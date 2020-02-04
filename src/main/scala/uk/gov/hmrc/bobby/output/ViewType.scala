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

import fansi.{Color, EscapeAttr, Str}
import uk.gov.hmrc.bobby.domain.Message
import uk.gov.hmrc.bobby.Util._
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}
import ViewType.messageColour

sealed trait ViewType {
  def headerNames: Seq[String]
  def renderMessage(m: Message): Seq[Str]
}

case object Flat extends ViewType {
  override def headerNames: Seq[String] = Seq("Level", "Dependency", "Via", "Your Version", "Outlawed Range", "Effective From", "Reason")

  override def renderMessage(m: Message): Seq[Str] = Seq(
    messageColour(m)(m.level.name),
    if(m.isPlugin) Color.Magenta(m.checked.moduleID.moduleName) else if (m.isLocal) Color.Green(m.checked.moduleID.moduleName) else Str(m.checked.moduleID.moduleName),
    Str(m.dependencyChain.lastOption.map(_.moduleName).getOrElse("")),
    Str(m.checked.moduleID.revision),
    Str(m.checked.result.rule.map(_.range.toString()).getOrElse("-")),
    Str(m.effectiveDate.map(_.toString).getOrElse("-")),
    Str(m.deprecationReason.map(_.toString).getOrElse("-"))
  )

}

case object Nested extends ViewType {
  override def headerNames: Seq[String] = Seq("Level", "Dependency", "Your Version", "Outlawed Range", "Latest Version", "Effective From")

  override def renderMessage(m: Message): Seq[Str] = Seq(
    messageColour(m)(m.level.name),
    if(m.isPlugin) Color.Magenta(m.moduleName) else if (m.isLocal) Color.Green(m.moduleName) else Str(s" => ${m.moduleName}"),
    Str(m.checked.moduleID.revision),
    Str(m.checked.result.rule.map(_.range.toString()).getOrElse("-")),
    Str(m.effectiveDate.map(_.toString).getOrElse("-"))
  )
}

case object Compact extends ViewType {
  override def headerNames: Seq[String] = Seq("Level", "Dependency", "Via", "Your Version", "Outlawed Range", "Effective From")

  override def renderMessage(m: Message): Seq[Str] = Seq(
    messageColour(m)(m.level.name),
    if(m.isPlugin) Color.Magenta(m.moduleName) else if (m.isLocal) Color.Green(m.moduleName) else Str(m.moduleName),
    Str(m.dependencyChain.lastOption.map(_.moduleName).getOrElse("")),
    Str(m.checked.moduleID.revision),
    Str(m.checked.result.rule.map(_.range.toString()).getOrElse("-")),
    Str(m.effectiveDate.map(_.toString).getOrElse("-"))
  )
}

object ViewType {

  def apply(s: String): ViewType = s match {
    case "Flat"     => Flat
    case "Nested"   => Nested
    case "Compact"  => Compact
    case _          => Compact
  }

  def messageColour(message: Message): EscapeAttr = message.level match {
    case ERROR => Color.Red
    case WARN => Color.Yellow
    case INFO => Color.Cyan
  }

}
