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
package uk.gov.hmrc

import sbt.Keys._
import sbt._
import uk.gov.hmrc.bobby.DependencyChecker
import uk.gov.hmrc.bobby.conf.DeprecatedDependencyConfiguration

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object SbtBobbyPlugin extends AutoPlugin {

  import uk.gov.hmrc.bobby.domain._

  val logger = ConsoleLogger()

  object autoImport {
    lazy val mandatoryFileUrl = settingKey[Option[String]]("file ")
  }

  def bobbyNexus = autoImport.mandatoryFileUrl in Global

  override def trigger = allRequirements

  import uk.gov.hmrc.SbtBobbyPlugin.autoImport._

  //TODO de-stringify and use the Version object everywhere
  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,

  //TODO get this from config
    mandatoryFileUrl := Some("some-url"),

    onLoad in Global := {
      findDependenciesWithNewerVersions(compactDependencies(libraryDependencies.value), scalaVersion.value) _ compose (onLoad in Global).value
    },
    onLoad in Global := {
      findDeprecatedDependencies(mandatoryFileUrl.value, compactDependencies(libraryDependencies.value)) _ compose (onLoad in Global).value
    }
  )

  def findDeprecatedDependencies(configFileOpt: Option[String], dependencies: Seq[ModuleID])(startState: State): State = {
    logger.info(s"[bobby] Checking for explicitly deprecated dependencies")
    configFileOpt.fold {
      logger.error("[bobby] Missing deprecated-dependencies configuration URL. See https://github.com/hmrc/bobby for details")
      startState.fail
    } { configFile =>

      logger.info(s"[bobby] Taking configuration from $configFile")
      val checker: DependencyChecker = DependencyChecker(DeprecatedDependencyConfiguration(new URL(configFile)))

      dependencies.foldLeft(startState) {
        case (currentState, module) => {
          checker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
            case MandatoryFail(latest) =>
              logger.error(s"[bobby] '${module.name} ${module.revision}' is deprecated and has to be upgraded! Reason: ${latest.reason}")
              currentState.exit(true)
            case MandatoryWarn(latest) =>
              logger.warn(s"[bobby] '${module.name} ${module.revision}' is deprecated! You will not be able to use it after ${latest.from}.  Reason: ${latest.reason}. Please consider upgrading")
              currentState
            case _ => currentState
          }
        }
      }
    }
  }

  // TODO, use warn if the version is really old
  def findDependenciesWithNewerVersions(dependencies: Seq[ModuleID], scalaVersion: String)(startState: State): State = {
    import uk.gov.hmrc.bobby.Nexus._
    logger.info(s"[bobby] Checking for out of date dependencies")

    findLocalNexusCreds(logger).fold {
      logger.error("[bobby] Could not find Nexus credentials in ~/.sbt/.credentials")
      startState.fail
    } { nexusCredentials =>

      logger.info(s"[bobby] using nexus at '${nexusCredentials.host}'")
      dependencies.foreach(module => {
        checkDependency(module, findLatestRevision(module, scalaVersion, nexusCredentials)) match {
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


