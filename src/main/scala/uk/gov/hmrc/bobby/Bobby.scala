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

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object Bobby extends Bobby {
  override val checker: DependencyChecker = DependencyChecker
  override val nexus: Option[Nexus] = Nexus(Configuration.credentials)
}

trait Bobby {

  val logger = ConsoleLogger()

  val checker: DependencyChecker
  val nexus: Option[Nexus]

  def validateDependencies(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject:Boolean)(state: State): State = {
    if(areDependenciesValid(dependencies, scalaVersion, isSbtProject)) state else state.exit(true)
  }

  def areDependenciesValid(dependencies: Seq[ModuleID], scalaVersion: String, isSbtProject:Boolean = false): Boolean = {

    logger.info(s"[bobby] Checking dependencies")
    if(isSbtProject){
      logger.info(s"[bobby] in SBT project, not checking for Nexus dependencies as Nexus search doesn't find SBT plugins")
    }
    val latestRevisions: Map[ModuleID, Option[String]] = getNexusRevisions(scalaVersion, compactDependencies(dependencies))

    outputNexusResults(latestRevisions, isSbtProject)

    val finalResult = doMandatoryCheck(latestRevisions)

    finalResult
  }

  def doMandatoryCheck(latestRevisions: Map[ModuleID, Option[String]]): Boolean = {
    latestRevisions.foldLeft(true) { case (result, (module, latestRevision)) => {
      checker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
        case MandatoryFail(latest) =>
          logger.error(buildErrorOutput(module, latest, latestRevision))
          false
        case MandatoryWarn(latest) =>
          logger.warn(s"[bobby] '${module.name} ${module.revision}' is deprecated! " +
            s"You will not be able to use it after ${latest.from}.  " +
            s"Reason: ${latest.reason}. Please consider upgrading" +
            s"${latestRevision.map(v => s" to '$v'").getOrElse("")}")
          result
        case _ => result //TODO unify DependencyCheckResult results
      }
    }}
  }

  def getNexusRevisions(scalaVersion: String, compacted: Seq[ModuleID]): Map[ModuleID, Option[String]] = {
    compacted.par.map { module =>
      module -> nexus.flatMap { n =>
        n.findLatestRevision(module, scalaVersion)
      }
    }.seq.toMap
  }

  def outputNexusResults(latestRevisions: Map[ModuleID, Option[String]], isSbtProject:Boolean): Unit = {
    latestRevisions.foreach { case (module, latestRevision) =>
      if (!isSbtProject && nexus.isDefined && latestRevision.isEmpty)
        logger.info(s"[bobby] Unable to get a latestRelease number for '${module.toString()}'")
      else if (!isSbtProject && latestRevision.isDefined && Version(latestRevision.get).isAfter(Version(module.revision)))
        logger.info(s"[bobby] '${module.name} ${module.revision}' is out of date, consider upgrading to '${latestRevision.get}'")
    }
  }

  def buildErrorOutput(module:ModuleID, dep:DeprecatedDependency, latestRevision:Option[String], prefix:String = "[bobby] "):String ={
    s"""
      ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
      |||
      |||    BOBBY FAILURE
      |||
      |||    The module '${module.name} ${module.name} ${module.revision}' is deprecated.
      |||
      |||    After ${dep.from} builds using it will fail.
      |||
      |||    ${dep.reason.replaceAll("\n", "\n|||\t")}
      |||
      |||    ${latestRevision.map(s => "Latest version is: " + s).getOrElse(" ")}
      |||
      ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
    """.stripMargin.replace("\n", "\n [bobby] ")
  }

  def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    dependencies
      .map { d => d -> s"${d.organization}.${d.name}" }
      .groupBy(_._2)
      .map { group => group._2.head._1 }
      .toSeq
  }
}
