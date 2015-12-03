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

package uk.gov.hmrc.bobby.domain

import org.joda.time.LocalDate
import sbt.ModuleID
import uk.gov.hmrc.bobby.Helpers
import Helpers._
import uk.gov.hmrc.bobby.domain.MessageLevels.{WARN, ERROR, INFO}

import scala.util.{Failure, Try}

object Message{
  val tabularHeader      = Seq("Level", "Dependency", "Your Version", "Invalid Range", "Latest Version", "Deadline", "Reason")
  val shortTabularHeader = Seq("Level", "Dependency", "Your Version", "Invalid Range", "Latest Version", "Deadline")

  implicit object MessageOrdering extends Ordering[Message] {
    def compare(a:Message, b:Message) = a.level compare b.level
  }
}

sealed trait Result
case object UnknownVersion extends Result
case object NewVersionAvailable extends Result
case object DependencyNearlyUnusable extends Result
case object DependencyUnusable extends Result

case class Message(result:Result, module: ModuleID, latestRevisionT: Try[Version], deprecationInfoO: Option[DeprecatedDependency]){

  val deprecationReason = deprecationInfoO.map(_.reason).getOrElse("-")
  val deprecationFrom = deprecationInfoO.map(_.from).getOrElse("-")
  val latestRevision = latestRevisionT.getOrElse("-")

  val moduleName = s"${module.organization}.${module.name}"

  val deadline:Option[LocalDate] = deprecationInfoO.map(_.from)

  val (level, tabularMessage) = result match {
    case UnknownVersion           => (INFO, s"Unable to get a latestRelease number for '${module.toString()}'")
    case NewVersionAvailable      => (INFO, "A new version is available")
    case DependencyNearlyUnusable => (WARN, deprecationReason)
    case DependencyUnusable       => (ERROR, deprecationReason)
  }

  val jsonMessage:String = result match {
    case UnknownVersion           => s"Unable to get a latestRelease number for '${module.toString()}'"
    case NewVersionAvailable      => s"${module.organization}.${module.name} ${module.revision}' is not the most recent version, consider upgrading to '$latestRevision'"
    case DependencyNearlyUnusable => s"${module.organization}.${module.name} ${module.revision} is deprecated: '$deprecationReason'. To be updated by $deprecationFrom to version $latestRevision"
    case DependencyUnusable       => deprecationReason
  }

  def isError: Boolean = level.equals(MessageLevels.ERROR)

  def rawJson = s"""{ "level" : "${level.name}", "message" : "$jsonMessage" }"""

  def shortTabularOutput:Seq[String] = Seq(
    level.toString,
    moduleName,
    module.revision,
    deprecationInfoO.map(_.range.toString()).getOrElse("-"),
    latestRevisionT.map(_.toString).getOrElseWith(_.getMessage),
    deadline.map(_.toString).getOrElse("-")
  )

  def longTabularOutput:Seq[String] = shortTabularOutput :+ tabularMessage

  def logOutput: (String, String) = level.name -> jsonMessage

}
