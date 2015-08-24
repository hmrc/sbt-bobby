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


object Bobby extends Bobby {
  override val checker: DependencyChecker = DependencyChecker
  override val repoSearch: RepoSearch = Nexus(Configuration.credentials).getOrElse{
    logger.info("[bobby] using maven search")
    MavenSearch
  }
}

trait Bobby {

  val logger = ConsoleLogger()

  val checker: DependencyChecker
  val repoSearch: RepoSearch

  def validateDependencies(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject:Boolean)(state: State): State = {
    if(areDependenciesValid(dependencies, scalaVersion, isSbtProject)) state else state.exit(true)
  }

  def areDependenciesValid(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject:Boolean = false): Boolean = {

    logger.info(s"[bobby] Checking dependencies")
    if(isSbtProject){
      logger.info(s"[bobby] in SBT project, not checking for Nexus dependencies as Nexus search doesn't find SBT plugins")
    }

    val latestRevisions: Map[ModuleID, Option[String]] = getNexusRevisions(scalaVersion, compactDependencies(dependencies))
    outputWarningsToConsole(calculateNexusResults(latestRevisions, isSbtProject))

    val mandatoryRevisionCheckResults = checkMandatoryDependencies(latestRevisions)
    outputWarningsToConsole(mandatoryRevisionCheckResults)

    doMandatoryCheck(mandatoryRevisionCheckResults)
  }

  def doMandatoryCheck(checkResults: List[(String, String)]): Boolean = {
    checkResults.foldLeft(true) { case (result, (messageType, messageText)) => {
      !messageType.equals("ERROR")
    }
    }
  }

  def checkMandatoryDependencies(latestRevisions: Map[ModuleID, Option[String]]): List[(String, String)] = {
    latestRevisions.toList.flatMap({
      case (module, latestRevision) =>
        checker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
          case MandatoryFail(exclusion) =>
            Some(("ERROR", buildErrorOutput(module, exclusion, latestRevision)))

          case MandatoryWarn(exclusion) =>
            Some(("WARN", s"'${module.name} ${module.revision}' is deprecated! " +
              s"You will not be able to use it after ${exclusion.from}.  " +
              s"Reason: ${exclusion.reason}. Please consider upgrading" +
              s"${latestRevision.map(v => s" to '$v'").getOrElse("")}"))
          case _ => None // TODO test coverage to ensure that flatten removes empty tuples
        }
    })
  }

  def getNexusRevisions(scalaVersion: String, compacted: Seq[ModuleID]): Map[ModuleID, Option[String]] = {
    compacted.par.map { module =>
      module -> repoSearch.findLatestRevision(module, Option(scalaVersion))
    }.seq.toMap
  }

  def calculateNexusResults(latestRevisions: Map[ModuleID, Option[String]], isSbtProject:Boolean): List[(String, String)] = {
    latestRevisions.toList.flatMap {
      case (module, latestRevision) =>
      if (!isSbtProject && latestRevision.isEmpty)
        Some(("INFO", s"Unable to get a latestRelease number for '${module.toString()}'"))
      else if (!isSbtProject && latestRevision.isDefined && Version(latestRevision.get).isAfter(Version(module.revision)))
        Some(("INFO", s"'${module.name} ${module.revision}' is out of date, consider upgrading to '${latestRevision.get}'"))
      else
        None
    }
  }

  def buildErrorOutput(module:ModuleID, dep:DeprecatedDependency, latestRevision:Option[String], prefix:String = "[bobby] "):String ={
    s"""The module '${module.name} ${module.name} ${module.revision}' is deprecated.\n\n""" +
      s"""After ${dep.from} builds using it will fail.\n\n${dep.reason.replaceAll("\n", "\n|||\t")}\n\n""" +
      s"""${latestRevision.map(s => "Latest version is: " + s).getOrElse(" ")}"""
  }

  def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    dependencies
      .map { d => d -> s"${d.organization}.${d.name}" }
      .groupBy(_._2)
      .map { group => group._2.head._1 }
      .toSeq
  }

  private def outputWarningsToConsole(messages: List[(String, String)]): Unit = {
    messages.foreach(message => {
      val messageType: String = message._1
      val text: String = "[bobby] " + message._2
      messageType match {
        case("ERROR") => renderConsoleErrorMessage(text)
        case("WARN") => logger.warn(text)
        case _ => logger.info(text)
      }
    })
  }

  def renderConsoleErrorMessage(text: String): Unit = {
    logger.error(text)
  }

  
}