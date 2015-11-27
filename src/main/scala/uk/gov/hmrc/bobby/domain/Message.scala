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
  val tabularHeader      = Seq("Level", "Dependency", "Your Version", "Latest Version", "Deadline", "Reason")
  val shortTabularHeader = Seq("Level", "Dependency", "Your Version", "Latest Version", "Deadline")

  implicit object MessageOrdering extends Ordering[Message] {
    def compare(a:Message, b:Message) = a.level compare b.level
  }
}



//trait Message {
//  def isError: Boolean = level.equals(MessageLevels.ERROR)
//
//  def jsonOutput: Map[String, String] = Map("level" -> level.name, "message" -> jsonMessage)
//
//  def shortTabularOutput:Seq[String] = Seq(
//    level.toString,
//    moduleName,
//    module.revision,
//    latestRevision.map(_.toString).getOrElseWith(_.getMessage),
//    deadline.map(_.toString).getOrElse("-")
//  )
//
//  def longTabularOutput:Seq[String] = shortTabularOutput :+ tabularMessage
//
//  def logOutput: (String, String) = level.name -> jsonMessage
//
//  def module:ModuleID
//
//  def moduleName = s"${module.organization}.${module.name}"
//
//  def level:MessageLevels.Level = MessageLevels.INFO
//
//  def jsonMessage: String
//
//  def tabularMessage:String
//
//  def deadline:Option[LocalDate] = None
//
//  def latestRevision: Try[Version]
//
//  override def toString():String = jsonMessage
//}
//
//class UnknownVersion(val module: ModuleID, reason:Throwable) extends Message {
//  val jsonMessage = s"Unable to get a latestRelease number for '${module.toString()}'"
//
//  val tabularMessage = s"Unable to get a latestRelease number for '${module.toString()}'"
//
//  val latestRevision: Try[Version] = Failure(reason)
//}
//
//class NewVersionAvailable(val module: ModuleID, override val latestRevision: Try[Version]) extends Message {
//  val jsonMessage = s"'${module.organization}.${module.name} ${module.revision}' is not the most recent version, consider upgrading to '${latestRevision.getOrElse("-")}'"
//
//  val tabularMessage = s"A new version is available"
//}
//
//class DependencyUnusable(val module: ModuleID, override val latestRevision: Try[Version], val deprecationInfo: DeprecatedDependency, prefix: String = "[bobby] ") extends Message {
//
//  override val level = MessageLevels.ERROR
//
//  val jsonMessage =
//    s"""${module.organization}.${module.name} ${module.revision} is deprecated.\n\n""" +
//      s"""After ${deprecationInfo.from} builds using it will fail.\n\n${deprecationInfo.reason.replaceAll("\n", "\n|||\t")}\n\n""" +
//      latestRevision.map(s => "Latest version is: " + s).getOrElseWith(_.getMessage)
//
//  val tabularMessage = deprecationInfo.reason
//
//  override val deadline = Option(deprecationInfo.from)
//}
//
//class DependencyNearlyUnusable(val module: ModuleID, override val latestRevision: Try[Version], val deprecationInfo: DeprecatedDependency) extends Message {
//
//  override val level = MessageLevels.WARN
//
//  val jsonMessage = s"${module.organization}.${module.name} ${module.revision} is deprecated: '${deprecationInfo.reason}'. To be updated by ${deprecationInfo.from} to version ${latestRevision.getOrElse("-")}"
//
//  val tabularMessage = deprecationInfo.reason
//
//  override val deadline = Option(deprecationInfo.from)
//}

sealed trait Result
case object UnknownVersion extends Result
case object NewVersionAvailable extends Result
case object DependencyNearlyUnusable extends Result
case object DependencyUnusable extends Result

case class Message(result:Result, module: ModuleID, latestRevisionT: Try[Version], deprecationInfoO: Option[DeprecatedDependency]){

  val deprecationReason = deprecationInfoO.map(_.reason).getOrElse("-")
  val deprecationFrom = deprecationInfoO.map(_.from).getOrElse("-")
  val latestRevision = latestRevisionT.getOrElse("-")

  val (level, tabularMessage) = result match {
    case UnknownVersion           => (INFO, "Unable to get a latestRelease number for '${module.toString()}'")
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

  def jsonOutput: Map[String, String] = Map("level" -> level.name, "message" -> jsonMessage)

  def shortTabularOutput:Seq[String] = Seq(
    level.toString,
    moduleName,
    module.revision,
    latestRevisionT.map(_.toString).getOrElseWith(_.getMessage),
    deadline.map(_.toString).getOrElse("-")
  )

  def longTabularOutput:Seq[String] = shortTabularOutput :+ tabularMessage

  def logOutput: (String, String) = level.name -> jsonMessage

  def moduleName = s"${module.organization}.${module.name}"

  def deadline:Option[LocalDate] = None

  //override def toString():String = jsonMessage
}
