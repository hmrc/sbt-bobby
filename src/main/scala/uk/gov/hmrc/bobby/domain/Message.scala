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

package uk.gov.hmrc.bobby.domain

import org.joda.time.LocalDate
import sbt.ModuleID
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}

object Message {
  val tabularHeader =
    Seq("Level", "Dependency", "Via", "Your Version", "Outlawed Range", "Latest Version", "Effective From", "Reason")
  val shortTabularHeader = Seq("Level", "Dependency", "Via", "Your Version", "Outlawed Range", "Latest Version", "Effective From")

  implicit object MessageOrdering extends Ordering[Message] {
    def compare(a: Message, b: Message) = a.level compare b.level
  }

}

case class Message(
  result: BobbyResult,
  module: ModuleID,
  dependencyChain: Seq[ModuleID],
  latestVersion: Option[Version]) {

  val deprecationReason = result.rule.map(_.reason).getOrElse("-")
  val deprecationFrom   = result.rule.map(_.from).getOrElse("-")
  val latestRevision    = latestVersion.getOrElse("-")
  val deadline: Option[LocalDate] =  result.rule.map(_.from)

  val moduleName = s"${module.organization}.${module.name}"

  def buildModuleName(moduleID: ModuleID) = s"${moduleID.organization}.${moduleID.name}"

  val level = result match {
    case BobbyOk            => INFO
    case BobbyWarning(_)    => WARN
    case BobbyViolation(_)  => ERROR
  }

  val jsonMessage: String = result match {
    case BobbyOk         => ""
    case BobbyWarning(r) =>
      s"${module.organization}.${module.name} ${module.revision} is deprecated: '${r.reason}'. To be updated by ${r.from} to version $latestRevision"
    case BobbyViolation(r) => r.reason
  }

  def isError: Boolean = level.equals(MessageLevels.ERROR)

  def rawJson: String =
    s"""{ "level" : "${level.name}",
       |  "message" : "$jsonMessage",
       |  "data": {
       |    "organisation" : "${module.organization}",
       |    "name" : "${module.name}",
       |    "revision" : "${module.revision}",
       |    "result" : "${result.getClass.getSimpleName}",
       |    "deprecationFrom" : "$deprecationFrom",
       |    "deprecationReason" : "$deprecationReason",
       |    "latestRevision" : "$latestRevision"
       |  }
       |}""".stripMargin

  def shortTabularOutput: Seq[String] = {
    Seq(
      level.name,
      moduleName,
      dependencyChain.lastOption.map(buildModuleName).getOrElse(""),
      module.revision,
      result.rule.map(_.range.toString()).getOrElse("-"),
      latestVersion.map(_.toString).getOrElse("?"),
      deadline.map(_.toString).getOrElse("-")
    )
  }

  def longTabularOutput: Seq[String] = shortTabularOutput :+ result.rule.map(_.reason).getOrElse("")

  def logOutput: (String, String) = level.name -> jsonMessage

}
