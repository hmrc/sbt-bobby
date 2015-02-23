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

import java.net.URL

import sbt.Keys._
import sbt._

import scala.io.Source
import scala.util.{Failure, Success, Try}

object SbtBobbyPlugin extends AutoPlugin {

  import uk.gov.hmrc.bobby.domain.Core._
  import uk.gov.hmrc.bobby.domain._

  val logger = ConsoleLogger()

  object autoImport {
    lazy val checkNexusDependencyVersions = taskKey[Try[Map[ModuleID, DependencyCheckResult]]]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val checkMandatoryDependencyVersionsInput = inputKey[Try[Map[ModuleID, DependencyCheckResult]]]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val checkMandatoryDependencyVersions = taskKey[Try[Map[ModuleID, DependencyCheckResult]]]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val mandatoryFileUrl = settingKey[Option[String]]("file ")
  }
//
//  def check = Command.args("mandatoryCheck", "<url>") { (state, args) =>
//    args.headOption.map { head =>
//      println("head = " + head)
//      val mandatories: Map[OrganizationName, String] = getMandatoryVersions(Source.fromURL(head).mkString)
//
//      val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
//        module -> getMandatoryResult(module, mandatories)
//      }.toMap
//
//      dependencyResults
//    }.getOrElse(state.fail)
//  }

  def bobbyNexus = autoImport.mandatoryFileUrl in Global

  override def trigger = allRequirements

  import uk.gov.hmrc.SbtBobbyPlugin.autoImport._

  //TODO de-stringify and use the Version object everywhere
  //TODO move more code into Core
  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,
    mandatoryFileUrl := None,
    checkMandatoryDependencyVersions := {
//      import sbt.complete.DefaultParsers._
//
//      val args: Seq[String] = spaceDelimited("<arg>").parsed
//
//      println("args = " + args)
//
//      args.headOption.map { mandatoryUrl =>
      mandatoryFileUrl.value.map { mandatoryUrl =>

        println("mandatoryUrl = " + mandatoryUrl)

        streams.value.log.debug(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
        val mandatories: Map[OrganizationName, String] = getMandatoryVersions(Source.fromURL(mandatoryUrl).mkString)

        val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
          module -> getMandatoryResult(module, mandatories)
        }.toMap

        dependencyResults
      }.toTry(new Exception("URL for mandatory dependency versions not supplied"))
    },
//    onLoad in Global := {
////      runDependencyCheckTask(checkNexusDependencyVersions) _ compose (onLoad in Global).value
//      runDependencyCheckTask(checkMandatoryDependencyVersions) _ compose (onLoad in Global).value
//    },

    checkNexusDependencyVersions := {
      streams.value.log.debug(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
      import uk.gov.hmrc.bobby.Nexus._

      findLocalNexusCreds(streams.value.log).map { nexusCreds =>
        streams.value.log.info(s"[bobby] using nexus at '${nexusCreds.host}'")

        val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
          module -> checkDependency(module, findLatestRevision(module, scalaVersion.value, nexusCreds))
        }.toMap

        dependencyResults
      }
    }
  )

  def runDependencyCheckTask(task:TaskKey[Try[Map[ModuleID, DependencyCheckResult]]])(startState:State):State={
    logger.info(s"[bobby] starting '${task.key.label}' task")

    def failAndExit(reason:Throwable):State = {
      logger.error("failed due to " + reason.getMessage)
      logger.trace(reason)
      startState.exit(false)
    }

    def sensibilizeTaskResult[A](a:Option[(State, Result[Try[A]])]):Try[(State, A)]={
      a.toTry(new Exception("problem executing task")).flatMap { case(s, r) =>
        r.toEither.toTry.flatten.map { a => (s, a)}
      }
    }

    val taskResult = sensibilizeTaskResult(Project.runTask(task, startState, false))

    val result: Try[State] = taskResult.map { case(state, result) =>
      outputResult(logger, result)
      state
    }

    result match {
      case Success(r) => r
      case Failure(e) => failAndExit(e)
    }
  }

  //TODO format this into some nice table
  def outputResult(out:ConsoleLogger, dependencyResults: Map[ModuleID, DependencyCheckResult]) {
    dependencyResults.foreach { case(module, result) => result match {
      case MandatoryFail(latest) => out.error(s"[bobby] Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
      case NotFoundInNexus       => out.info(s"[bobby] Unable to get a latestRelease number for '${module.toString()}'")
      case NexusHasNewer(latest) => out.warn(s"[bobby] Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
      case _ =>
    }}
    
    dependencyResults.values.find {
      _.fail
    }.fold(
        out.success("[bobby] No invalid dependencies")
    ) { fail =>
        out.warn("[bobby] One or more dependency version check failed, see previous error.\nThis means you are using an out-of-date library which is not allowed in services deployed on the Tax Platform.")
      }
  }


  def getMandatoryResult(module:ModuleID, mandatories: Map[OrganizationName, String]): DependencyCheckResult ={
    mandatories.get(OrganizationName(module)) match {
      case Some(mandatoryVersion) if mandatoryVersion > module.revision => MandatoryFail(mandatoryVersion)
      case _ => OK
    }
  }

  implicit class OptionPimp[A](o:Option[A]){
    def toTry(ex:Exception):Try[A] = o.map(Success(_)).getOrElse(Failure(ex))
  }

  implicit class EitherPimp[L <: Throwable,R](e:Either[L,R]){
    def toTry:Try[R] = e.fold(Failure(_), Success(_))
  }
}


