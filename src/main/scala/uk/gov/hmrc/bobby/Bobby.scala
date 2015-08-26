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

import java.io.{File, PrintWriter}

import play.api.libs.json.Json
import sbt.{ConsoleLogger, ModuleID, State}
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.JsonOutingFileWriter

object Bobby extends Bobby {
  override val checker: DependencyChecker = DependencyChecker
  override val repoSearch: RepoSearch = Nexus(Configuration.credentials).getOrElse {
    logger.info("[bobby] using maven search")
    MavenSearch
  }
  override val jsonOutputFileWriter = JsonOutingFileWriter
}

trait Bobby {

  val logger = ConsoleLogger()

  val checker: DependencyChecker
  val repoSearch: RepoSearch
  val jsonOutputFileWriter: JsonOutingFileWriter


  def validateDependencies(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject: Boolean)(state: State): State = {
    if (areDependenciesValid(dependencies, scalaVersion, isSbtProject)) state else state.exit(true)
  }

  def areDependenciesValid(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject: Boolean): Boolean = {

    logger.info(s"[bobby] Checking dependencies")
    if (isSbtProject) {
      logger.info(s"[bobby] in SBT project, not checking for Nexus dependencies as Nexus search doesn't find SBT plugins")
    }

    val latestRevisions: Map[ModuleID, Option[Version]] = getNexusRevisions(scalaVersion, compactDependencies(dependencies))
    val nexusResults =
      if (isSbtProject) List.empty[Message]
      else calculateNexusResults(latestRevisions)

    val mandatoryRevisionCheckResults = checkMandatoryDependencies(latestRevisions)

    val messages: List[Message] = nexusResults ++ mandatoryRevisionCheckResults

    outputMessagesToConsole(messages)
    jsonOutputFileWriter.outputMessagesToJsonFile(messages)

    noErrorsExist(mandatoryRevisionCheckResults)
  }

  def noErrorsExist(results: List[Message]): Boolean = ! results.exists(_.isError)


  def checkMandatoryDependencies(latestRevisions: Map[ModuleID, Option[Version]]): List[Message] = {
    latestRevisions.toList.flatMap({
      case (module, latestRevision) =>
        checker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
          case MandatoryFail(exclusion) =>
            Some(new DependencyUnusable(module, exclusion, latestRevision))

          case MandatoryWarn(exclusion) =>
            Some(new DependencyNearlyUnusable(module, exclusion, latestRevision))

          case _ => None
        }
    })
  }

  def getNexusRevisions(scalaVersion: String, compacted: Seq[ModuleID]): Map[ModuleID, Option[Version]] = {
    compacted.par.map { module =>
      module -> repoSearch.findLatestRevision(module, Option(scalaVersion))
    }.seq.toMap
  }

  def calculateNexusResults(latestRevisions: Map[ModuleID, Option[Version]]): List[Message] = {
    latestRevisions.toList.flatMap {
      case (module, None) =>
        Some(new UnknownVersion(module))

      case (module, Some(latestRevision)) if latestRevision.isAfter(Version(module.revision)) =>
        Some(new DependencyOutOfDate(module, latestRevision))

      case _ =>
        None
    }
  }

  def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    def fullyQualifiedName(d: ModuleID) = s"${d.organization}.${d.name}"

    dependencies
      .groupBy(fullyQualifiedName)
      .map(_._2.head)
      .toSeq
  }

  private def outputMessagesToConsole(messages: List[Message]): Unit = {
    messages.map(_.logOutput).foreach(message => {
      val messageType = message._1
      val text = "[bobby] " + message._2
      messageType match {
        case "ERROR" => renderConsoleErrorMessage(text)
        case "WARN" => logger.warn(text)
        case _ => logger.info(text)
      }
    })
  }

  def renderConsoleErrorMessage(text: String): Unit = {
    logger.error(text)
  }

}

trait Message {
  def isError: Boolean = level.equals("ERROR")

  def jsonOutput: Map[String, String] = Map("level" -> level, "message" -> message)

  def logOutput: (String, String) = level -> message

  def level = "INFO"

  def message: String

}

class UnknownVersion(module: ModuleID) extends Message {
  val message = s"Unable to get a latestRelease number for '${module.toString()}'"
}

class DependencyOutOfDate(module: ModuleID, latestRevision: Version) extends Message {
  val message = s"'${module.name} ${module.revision}' is out of date, consider upgrading to '$latestRevision'"
}

class DependencyUnusable(module: ModuleID, dep: DeprecatedDependency, latestRevision: Option[Version], prefix: String = "[bobby] ") extends Message {

  override val level: String = "ERROR"

  val message =
    s"""The module '${module.name} ${module.name} ${module.revision}' is deprecated.\n\n""" +
      s"""After ${dep.from} builds using it will fail.\n\n${dep.reason.replaceAll("\n", "\n|||\t")}\n\n""" +
      latestRevision.map(s => "Latest version is: " + s).getOrElse(" ")
}

class DependencyNearlyUnusable(module: ModuleID, exclusion: DeprecatedDependency, latestRevision: Option[Version]) extends Message {

  override val level: String = "WARN"

  val message = s"'${module.name} ${module.revision}' is deprecated! " +
    s"You will not be able to use it after ${exclusion.from}.  " +
    s"Reason: ${exclusion.reason}. Please consider upgrading" +
    latestRevision.map(v => s" to '$v'").getOrElse("")
}

