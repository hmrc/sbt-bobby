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

object DefaultRendering {
  implicit class ExtendedMessage(m: Message) {

    private val prefix = Str(m match {
      case _ if !m.isLocal => " T"    //transitive
      case _ if m.isPlugin => " P"    //plugin
      case _ => " L"                  //local
    })

    val levelStr: Str = messageColour(m)(m.level.name)
    val dependencyStr: Str = {
      val dep = s"${m.checked.moduleID.moduleName}$prefix"
      if(m.isPlugin) Color.Magenta(dep) else if (m.isLocal) Color.Blue(dep) else Str(dep)
    }
    val viaStr: Str = Str(m.dependencyChain.lastOption.map(_.moduleName).getOrElse(""))
    val yourVersionStr: Str = Str(m.checked.moduleID.revision)
    val outlawedRangeStr: Str = Str(m.checked.result.rule.map(_.range.toString()).getOrElse("-"))
    val effectiveDateStr: Str = Str(m.effectiveDate.map(_.toString).getOrElse("-"))
    val reasonStr: Str = Str(m.deprecationReason.map(_.toString).getOrElse("-"))
  }
}

import DefaultRendering._
case object Flat extends ViewType {
  override def headerNames: Seq[String] = Seq("Level", "Dependency", "Via", "Your Version", "Outlawed Range", "Effective From", "Reason")

  override def renderMessage(m: Message): Seq[Str] = Seq(
    m.levelStr,
    m.dependencyStr,
    m.viaStr,
    m.yourVersionStr,
    m.outlawedRangeStr,
    m.effectiveDateStr,
    m.reasonStr
  )

}

case object Nested extends ViewType {
  override def headerNames: Seq[String] = Seq("Level", "Dependency", "Your Version", "Outlawed Range", "Effective From")

  override def renderMessage(m: Message): Seq[Str] = Seq(
    messageColour(m)(m.levelStr),
    if(m.isLocal || m.isPlugin) m.dependencyStr else Str(s" => ${m.dependencyStr}"),
    m.yourVersionStr,
    m.outlawedRangeStr,
    m.effectiveDateStr
  )
}

case object Compact extends ViewType {
  override def headerNames: Seq[String] = Seq("Level", "Dependency", "Type", "Your Version", "Outlawed Range", "Effective From")

  override def renderMessage(m: Message): Seq[Str] = Seq(
    m.levelStr,
    m.dependencyStr,
    m.viaStr,
    m.yourVersionStr,
    m.outlawedRangeStr,
    m.effectiveDateStr
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
    case INFO => Color.Green
  }

}
