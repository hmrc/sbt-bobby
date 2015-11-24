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

package uk.gov.hmrc.bobby

import java.net.URL

import org.joda.time.LocalDate
import sbt.{ConsoleLogger, ModuleID}
import uk.gov.hmrc.SbtBobbyPlugin.Repo
import uk.gov.hmrc.bobby.Helpers._
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{JsonOutingFileWriter, Tabulator, TextOutingFileWriter}

import scala.util.{Failure, Try}



class BobbyValidationFailedException(message:String) extends RuntimeException(message)

object Bobby {

  val logger = ConsoleLogger()


//  val repoSearch: RepoSearch

  val blackListModuleOrgs = Set(
//    "com.typesafe.play",
//    "com.kenshoo",
//    "com.codahale.metrics",
    "org.scala-lang"
  )

  def validateDependencies(
                            dependencies: Seq[ModuleID],
                            scalaVersion: String,
                            reposValue:Seq[Repo],
                            checkForLatest: Boolean,
                            deprecatedDependenciesUrl: Option[URL] = None,
                            jsonOutputFileOverride:Option[String] = None,
                            isSbtProject: Boolean = false) = {

    dependencies foreach println

    val currentVersion = getClass.getPackage.getImplementationVersion
    logger.info(s"[bobby] Bobby version $currentVersion using repositories: ${reposValue.mkString(", ")}")

    val config = new Configuration(deprecatedDependenciesUrl, jsonOutputFileOverride)

    val prepared = prepareDependencies(dependencies, blackListModuleOrgs)

    val repoSearchO  = Repositories.buildAggregateRepositories(reposValue, checkForLatest)

    val latestRevisions: Option[Map[ModuleID, Try[Version]]] = repoSearchO.map { rs =>
      getLatestRepoRevisions(scalaVersion, prepared, rs)
    }

    val messages = ResultBuilder.calculate(prepared, config.loadDeprecatedDependencies, latestRevisions)

    Output.output(messages, config.jsonOutputFile, config.textOutputFile)

    messages.exists(_.isError)

    if (!messages.exists(_.isError))
      throw new BobbyValidationFailedException("See previous bobby output for more information")
  }

  private[bobby] def prepareDependencies(dependencies: Seq[ModuleID], blackListModuleOrgs:Set[String]): Seq[ModuleID] = {
    compactDependencies(dependencies)
      .filterNot(m => blackListModuleOrgs.contains(m.organization))
  }

  private[bobby] def getLatestRepoRevisions(
                         scalaVersion: String,
                         compacted: Seq[ModuleID],
                         repoSearch:RepoSearch
                       ): Map[ModuleID, Try[Version]] = {
    compacted.par.map { module =>
      module -> repoSearch.findLatestRevision(module, Option(scalaVersion))
    }.seq.toMap
  }


  def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    def orgAndName(d: ModuleID) = s"${d.organization}.${d.name}"

    dependencies
      .groupBy(orgAndName)
      .map(_._2.head)
      .toSeq
  }

object Output {

  def output(messages:List[Message], jsonFilePath:String, textFilePath:String): Unit ={
    messages.foreach { res =>
      println("all   " + res.longTabularOutput)
    }
    val jsonOutputFileWriter = new JsonOutingFileWriter(jsonFilePath)
    val textOutputFileWriter = new TextOutingFileWriter(textFilePath)


    outputMessagesToConsole(messages)
    jsonOutputFileWriter.outputMessagesToJsonFile(messages)
    textOutputFileWriter.outputMessagesToTextFile(messages)
  }

  private def outputMessagesToConsole(messages: List[Message]): Unit = {
    val model = messages
      .sortBy(_.moduleName)
      .sortWith((a, b) => LogLevels.compare(a.level, b.level))
      .map { m => m.shortTabularOutput }

    logger.info("[bobby] Bobby info and warnings. See bobby report artefact for more info.")



    Tabulator.formatAsStrings(Message.shortTabularHeader +: model).foreach { log =>
      logger.info(log)
    }

    messages.filter(_.isError).foreach { log =>
      renderConsoleErrorMessage(log.jsonMessage)
    }
  }

  def renderConsoleErrorMessage(text: String): Unit = {
    logger.error("-")
    logger.error("- Bobby mandatory failure details:")
    logger.error("-")
    logger.error(text)
    logger.error("")
  }
}
}

object LogLevels {

  sealed abstract class Level(
                                val order: Int,
                                val name: String) extends Ordered[Level] {

    def compare(that: Level) = this.order - that.order

    override def toString = name
  }

  def compare(a:Level, b:Level):Boolean = a.compare(b) < 0

  case object ERROR extends Level(0, "ERROR")
  case object WARN extends Level(1, "WARN")
  case object INFO extends Level(2, "INFO")

}

object Message{
  val tabularHeader      = Seq("Level", "Dependency", "Your Version", "Latest Version", "Deadline", "Reason")
  val shortTabularHeader = Seq("Level", "Dependency", "Your Version", "Latest Version", "Deadline")

  implicit object MessageOrdering extends Ordering[Message] {
    def compare(a:Message, b:Message) = a.level compare b.level
  }
}


trait Message {
  def isError: Boolean = level.equals(LogLevels.ERROR)

  def jsonOutput: Map[String, String] = Map("level" -> level.name, "message" -> jsonMessage)

  def shortTabularOutput = Seq(
    level,
    moduleName,
    module.revision,
    latestRevision.map(_.toString).getOrElseWith(_.getMessage),
    deadline.map(_.toString).getOrElse("-")
  )

  def longTabularOutput = Seq(
    level,
    moduleName,
    module.revision,
    latestRevision.map(_.toString).getOrElseWith(_.getMessage),
    deadline.map(_.toString).getOrElse("-"),
    tabularMessage
  )

  def logOutput: (String, String) = level.name -> jsonMessage

  def module:ModuleID

  def moduleName = s"${module.organization}.${module.name}"

  def level:LogLevels.Level = LogLevels.INFO

  def jsonMessage: String

  def tabularMessage:String

  def deadline:Option[LocalDate] = None

  def latestRevision: Try[Version]

  override def toString():String = jsonMessage
}

class UnknownVersion(val module: ModuleID, reason:Throwable) extends Message {
  val jsonMessage = s"Unable to get a latestRelease number for '${module.toString()}'"

  val tabularMessage = s"Unable to get a latestRelease number for '${module.toString()}'"

  val latestRevision: Try[Version] = Failure(reason)
}

class NewVersionAvailable(val module: ModuleID, override val latestRevision: Try[Version]) extends Message {
  val jsonMessage = s"'${module.organization}.${module.name} ${module.revision}' is not the most recent version, consider upgrading to '${latestRevision.getOrElse("-")}'"

  val tabularMessage = s"A new version is available"
}

class DependencyUnusable(val module: ModuleID, override val latestRevision: Try[Version], val deprecationInfo: DeprecatedDependency, prefix: String = "[bobby] ") extends Message {

  override val level = LogLevels.ERROR

  val jsonMessage =
    s"""${module.organization}.${module.name} ${module.revision} is deprecated.\n\n""" +
      s"""After ${deprecationInfo.from} builds using it will fail.\n\n${deprecationInfo.reason.replaceAll("\n", "\n|||\t")}\n\n""" +
      latestRevision.map(s => "Latest version is: " + s).getOrElseWith(_.getMessage)

  val tabularMessage = deprecationInfo.reason

  override val deadline = Option(deprecationInfo.from)
}

class DependencyNearlyUnusable(val module: ModuleID, override val latestRevision: Try[Version], val deprecationInfo: DeprecatedDependency) extends Message {

  override val level = LogLevels.WARN

  val jsonMessage = s"${module.organization}.${module.name} ${module.revision} is deprecated: '${deprecationInfo.reason}'. To be updated by ${deprecationInfo.from} to version ${latestRevision.getOrElse("-")}"

  val tabularMessage = deprecationInfo.reason

  override val deadline = Option(deprecationInfo.from)
}

