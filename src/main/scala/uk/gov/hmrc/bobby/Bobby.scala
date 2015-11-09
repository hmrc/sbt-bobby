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

import sbt.{ConsoleLogger, ModuleID, State}
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{JsonOutingFileWriter, TextOutingFileWriter}


object Bobby extends Bobby {
  override val checker: DependencyChecker = DependencyChecker

  override val repoSearch = new AggregateRepoSearch() {
    val repoName = "aggregate"
    override val repos: Seq[RepoSearch] = Seq(
      Bintray(Configuration.bintrayCredetials),
      Nexus(Configuration.nexusCredetials),
      Some(Maven)
    ).flatten

    val currentVersion = getClass.getPackage.getImplementationVersion
    logger.info(s"[bobby] Bobby version $currentVersion using repositories: ${repos.map(_.repoName).mkString(", ")}")
  }
  override val jsonOutputFileWriter = JsonOutingFileWriter
  override val textOutputFileWriter = TextOutingFileWriter
}

trait Bobby {

  val logger = ConsoleLogger()

  val checker: DependencyChecker
  val repoSearch: RepoSearch
  val jsonOutputFileWriter: JsonOutingFileWriter
  val textOutputFileWriter: TextOutingFileWriter


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
    textOutputFileWriter.outputMessagesToTextFile(messages)

    noErrorsExist(mandatoryRevisionCheckResults)
  }

  def noErrorsExist(results: List[Message]): Boolean = ! results.exists(_.isError)


  def checkMandatoryDependencies(latestRevisions: Map[ModuleID, Option[Version]]): List[Message] = {
    latestRevisions.toList.flatMap({
      case (module, latestRevision) =>
        checker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
          case MandatoryFail(exclusion) =>
            Some(new DependencyUnusable(module, latestRevision, exclusion))

          case MandatoryWarn(exclusion) =>
            Some(new DependencyNearlyUnusable(module, latestRevision, exclusion))

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
        Some(new DependencyOutOfDate(module, Some(latestRevision)))

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

object Message{
  val tabularHeader = Seq("Level", "Dependency", "Your Version", "Latest Version", "Deadline", "Information")
}

trait Message {
  def isError: Boolean = level.equals("ERROR")

  def jsonOutput: Map[String, String] = Map("level" -> level, "message" -> message)

  def tabularOutput = Seq(
    level,
    s"${module.organization}.${module.name}",
    module.revision,
    latestRevision.map(_.toString).getOrElse("(not-found)"),
    "-",
    message
  )

  def logOutput: (String, String) = level -> message

  def module:ModuleID

  def level = "INFO"

  def message: String

  def latestRevision: Option[Version]
}

trait MessageWithInfo extends Message {

  def deprecationInfo: DeprecatedDependency

  override def tabularOutput = Seq(
    level,
    s"${module.organization}.${module.name}",
    module.revision,
    latestRevision.map(_.toString).getOrElse("(not-found)"),
    deprecationInfo.from.toString,
    message
  )
}

class UnknownVersion(val module: ModuleID) extends Message {
  val message = s"Unable to get a latestRelease number for '${module.toString()}'"
  val latestRevision: Option[Version] = None
}

class DependencyOutOfDate(val module: ModuleID, val latestRevision: Option[Version]) extends Message {
  val message = s"'${module.name} ${module.revision}' is out of date, consider upgrading to '${latestRevision.getOrElse("-")}'"
  val deprecationInfo = None
}

class DependencyUnusable(val module: ModuleID, val latestRevision: Option[Version], val deprecationInfo: DeprecatedDependency, prefix: String = "[bobby] ") extends Message {

  override val level: String = "ERROR"

  val message =
    s"""The module '${module.name} ${module.name} ${module.revision}' is deprecated.\n\n""" +
      s"""After ${deprecationInfo.from} builds using it will fail.\n\n${deprecationInfo.reason.replaceAll("\n", "\n|||\t")}\n\n""" +
      latestRevision.map(s => "Latest version is: " + s).getOrElse(" ")
}

class DependencyNearlyUnusable(val module: ModuleID, val latestRevision: Option[Version], val deprecationInfo: DeprecatedDependency) extends MessageWithInfo {

  override val level: String = "WARN"

  val message = s"'${module.name} ${module.revision}' is deprecated! " +
    s"You will not be able to use it after ${deprecationInfo.from}.  " +
    s"Reason: ${deprecationInfo.reason}. Please consider upgrading" +
    latestRevision.map(v => s" to '$v'").getOrElse("")
}

