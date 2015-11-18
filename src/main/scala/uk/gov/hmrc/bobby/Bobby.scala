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

import org.joda.time.LocalDate
import sbt.{ConsoleLogger, ModuleID, State}
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{Tabulator, JsonOutingFileWriter, TextOutingFileWriter}


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

class BobbyValidationFailedException(message:String) extends RuntimeException(message)

trait Bobby {

  val logger = ConsoleLogger()

  val checker: DependencyChecker
  val repoSearch: RepoSearch
  val jsonOutputFileWriter: JsonOutingFileWriter
  val textOutputFileWriter: TextOutingFileWriter

  val blackListModuleOrgs = Set(
    "com.typesafe.play",
    "com.kenshoo",
    "com.codahale.metrics",
    "org.scala-lang"
  )

  def validateDependencies(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject: Boolean) = {
    if (!areDependenciesValid(dependencies, scalaVersion, isSbtProject, blackListModuleOrgs))
      throw new BobbyValidationFailedException("See previous bobby output for more information")
  }

  def areDependenciesValid(
                            dependencies: Seq[ModuleID],
                            scalaVersion: String,
                            isSbtProject: Boolean,
                            blackListModuleOrgs:Set[String] = Set.empty[String]): Boolean = {

    logger.info(s"[bobby] Checking dependencies")
    if (isSbtProject) {
      logger.info(s"[bobby] in SBT project, not checking for Nexus dependencies as Nexus search doesn't find SBT plugins")
    }

    val prepared = prepareDependencies(dependencies, blackListModuleOrgs)
    val latestRevisions: Map[ModuleID, Option[Version]] = getNexusRevisions(scalaVersion, prepared)
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

  private[bobby] def prepareDependencies(dependencies: Seq[ModuleID], blackListModuleOrgs:Set[String]): Seq[ModuleID] = {
    compactDependencies(dependencies)
      .filterNot(m => blackListModuleOrgs.contains(m.organization))
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
        Some(new NewVersionAvailable(module, Some(latestRevision)))

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
    latestRevision.map(_.toString).getOrElse("-"),
    deadline.map(_.toString).getOrElse("-")
  )

  def longTabularOutput = Seq(
    level,
    moduleName,
    module.revision,
    latestRevision.map(_.toString).getOrElse("(not-found)"),
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

  def latestRevision: Option[Version]
}

class UnknownVersion(val module: ModuleID) extends Message {
  val jsonMessage = s"Unable to get a latestRelease number for '${module.toString()}'"

  val tabularMessage = s"Unable to get a latestRelease number for '${module.toString()}'"

  val latestRevision: Option[Version] = None
}

class NewVersionAvailable(val module: ModuleID, val latestRevision: Option[Version]) extends Message {
  val jsonMessage = s"'${module.organization}.${module.name} ${module.revision}' is not the most recent version, consider upgrading to '${latestRevision.getOrElse("-")}'"

  val tabularMessage = s"A new version is available"
}

class DependencyUnusable(val module: ModuleID, val latestRevision: Option[Version], val deprecationInfo: DeprecatedDependency, prefix: String = "[bobby] ") extends Message {

  override val level = LogLevels.ERROR

  val jsonMessage =
    s"""${module.organization}.${module.name} ${module.revision} is deprecated.\n\n""" +
      s"""After ${deprecationInfo.from} builds using it will fail.\n\n${deprecationInfo.reason.replaceAll("\n", "\n|||\t")}\n\n""" +
      latestRevision.map(s => "Latest version is: " + s).getOrElse(" ")

  val tabularMessage = deprecationInfo.reason

  override val deadline = Option(deprecationInfo.from)
}

class DependencyNearlyUnusable(val module: ModuleID, val latestRevision: Option[Version], val deprecationInfo: DeprecatedDependency) extends Message {

  override val level = LogLevels.WARN

  val jsonMessage = s"${module.organization}.${module.name} ${module.revision} is deprecated: '${deprecationInfo.reason}'. To be updated by ${deprecationInfo.from} to version ${latestRevision.getOrElse("-")}"

  val tabularMessage = deprecationInfo.reason

  override val deadline = Option(deprecationInfo.from)
}

