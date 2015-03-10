/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.bobby

import java.net.URL

import sbt.{ConsoleLogger, ModuleID, State}
import uk.gov.hmrc.bobby.conf.{ConfigFile, DeprecatedDependencyConfiguration}
import uk.gov.hmrc.bobby.domain._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Bobby {

  val logger = ConsoleLogger()

  def findDeprecatedDependencies(dependencies: Seq[ModuleID], scalaVersion: String)(startState: State): State = {
    logger.info(s"[bobby] Checking for explicitly deprecated dependencies")

    val compacted = compactDependencies(dependencies)

    val ddlfc = System.getProperty("user.home") + "/.sbt/bobby.conf"

    val bobbyConfig: Option[String] =  new ConfigFile(ddlfc).get("deprecated-dependencies")
    val nexusConfig = Nexus.findLocalNexusCreds()

    if(bobbyConfig.isEmpty) {
      logger.warn(s"deprecated-dependencies configuration not found. Continuing without checking for deprecated dependencies")
      startState
    } else if( nexusConfig.isEmpty){
      logger.error("[bobby] Could not find Nexus credentials in ~/.sbt/.credentials")
      startState.exit(true)
    } else {

      val checker: DependencyChecker = DependencyChecker(DeprecatedDependencyConfiguration(new URL(bobbyConfig.get)))

      compacted.foldLeft(startState) {
        case (currentState, module) => {
          checker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
            case MandatoryFail(latest) =>
              val latestRevision: Option[String] = Nexus.findLatestRevision(module, scalaVersion, nexusConfig.get)
              logger.error(s"[bobby] '${module.name} ${module.revision}' is deprecated and has to be upgraded! " +
                s"Reason: ${latest.reason}. " +
                s"${latestRevision.map(v => s"Please consider using '$v' instead").getOrElse("")}")
              currentState.exit(true)
            case MandatoryWarn(latest) =>
              val latestRevision: Option[String] = Nexus.findLatestRevision(module, scalaVersion, nexusConfig.get)
              logger.warn(s"[bobby] '${module.name} ${module.revision}' is deprecated! " +
                s"You will not be able to use it after ${latest.from}.  " +
                s"Reason: ${latest.reason}. Please consider upgrading" +
                s"${latestRevision.map(v => s"to '$v'").getOrElse("")}")
              currentState
            case _ => currentState
          }
        }
      }
    }
  }

  // TODO, use warn if the version is really old
  def findDependenciesWithNewerVersions(dependencies: Seq[ModuleID], scalaVersion: String)(startState: State): State = {
    logger.info(s"[bobby] Checking for out of date dependencies")

    val compacted = compactDependencies(dependencies)

    Nexus.findLocalNexusCreds().fold {
      logger.error("[bobby] Could not find Nexus credentials in ~/.sbt/.credentials")
      startState.fail
    } { nexusCredentials =>

      logger.info(s"[bobby] using nexus at '${nexusCredentials.host}'")
      compacted.foreach(module => {
        Nexus.checkDependency(module, Nexus.findLatestRevision(module, scalaVersion, nexusCredentials)) match {
          case NotFoundInNexus =>
            logger.info(s"[bobby] Unable to get a latestRelease number for '${module.toString()}'")
          case NexusHasNewer(latest) =>
            logger.info(s"[bobby] '${module.name} ${module.revision}' is out of date, consider upgrading to '$latest'")
          case _ =>
        }
      })
      startState
    }
  }


  def compactDependencies(dependencies: Seq[ModuleID]) = {

    val b = new ListBuffer[ModuleID]()
    val seen = mutable.HashSet[String]()
    for (x <- dependencies) {
      if (!seen(s"${x.organization}.${x.name}" )) {
        b += x
        seen += s"${x.organization}.${x.name}"
      }
    }
    b.toSeq
  }
}
