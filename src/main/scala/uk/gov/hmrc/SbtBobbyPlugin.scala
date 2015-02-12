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
import sbt.Success
import sbt._
import uk.gov.hmrc.Core.Version

import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}

object SbtBobbyPlugin extends AutoPlugin {

  import Core._
  import Version._

  object autoImport {
    lazy val checkNexusDependencyVersions = taskKey[Try[Map[ModuleID, DependencyCheckResult]]]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val checkMandatoryDependencyVersions = taskKey[Unit]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val mandatoryUrl = settingKey[Option[URL]]("URL for mandatory dependencies")
  }
  
  override def trigger = allRequirements

  import autoImport._

  //TODO de-stringify and use the Version object everywhere
  //TODO move more code into Core
  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,
    mandatoryUrl := Some(new File("../bobby/src/test/resources/mandatory-example.txt").toURI.toURL),
    checkMandatoryDependencyVersions := {
      if(mandatoryUrl.value.isEmpty) streams.value.log.warn(s"[bobby] No setting for ${mandatoryUrl.key.label}, skipping mandatory check")

      mandatoryUrl.value.foreach { mandatoryUrl =>
        streams.value.log.debug(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
        val mandatories: Map[OrganizationName, String] = getMandatoryVersions(Source.fromURL(mandatoryUrl).mkString)

        val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
          module -> getMandatoryResult(module, mandatories)
        }.toMap

        outputResult(null/*streams.value*/, dependencyResults)
      }
    },
    onLoad in Global := {
      runDependencyCheckTask(checkNexusDependencyVersions) _ compose (onLoad in Global).value
    },

    checkNexusDependencyVersions := {
      streams.value.log.debug(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
      val nexus = findLocalNexusCreds(streams.value.log)

      nexus.map { nexusRepo =>
        streams.value.log.info(s"[bobby] using nexus at '${nexusRepo.host}'")

        val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
          module -> getNexusResult(module, findLatestRevision(module, scalaVersion.value, nexusRepo))
        }.toMap

        dependencyResults
      }
    }
  )

  def runDependencyCheckTask(task:TaskKey[Try[Map[ModuleID, DependencyCheckResult]]])(startState:State):State={
    val logger = ConsoleLogger()
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

  sealed trait DependencyCheckResult {
    def fail:Boolean
  }
  trait Fail extends DependencyCheckResult { val fail = true}
  trait Pass extends DependencyCheckResult { val fail = false}

  case class MandatoryFail(latest:String) extends DependencyCheckResult with Fail
  case class NexusHasNewer(latest:String) extends DependencyCheckResult with Pass
  object NotFoundInNexus extends DependencyCheckResult with Pass
  object OK extends DependencyCheckResult with Pass


  def getMandatoryResult(module:ModuleID, mandatories: Map[OrganizationName, String]): DependencyCheckResult ={
    mandatories.get(OrganizationName(module)) match {
      case Some(mandatoryVersion) if mandatoryVersion > module.revision => MandatoryFail(mandatoryVersion)
      case _ => OK
    }
  }
  // TODO fail if nexus version is 2 or more major releases behind
  def getNexusResult(module:ModuleID, latestNexusRevision:Option[String]): DependencyCheckResult ={

    latestNexusRevision match {
      case None => NotFoundInNexus
      case Some(latestNexus) if latestNexus > module.revision => NexusHasNewer(latestNexus)
      case Some(latestNexus) => OK
    }
  }

  def shortenScalaVersion(scalaVersion : String):String = {
    scalaVersion.split('.') match {
      case Array(major, minor, _*) => major + "." + minor
    }
  }

  private def getSearchTerms(versionInformation: ModuleID, scalaVersion : String) : String = {
    val shortenedScalaVersion = shortenScalaVersion(scalaVersion)
    s"${versionInformation.name}_$shortenedScalaVersion&g=${versionInformation.organization}"
  }

  //TODO test nexus connection and fail if we can't connect
  private def findLatestRevision(versionInformation: ModuleID, scalaVersion : String, nexus : NexusCredentials): Option[String] = {
    val query = s"https://${nexus.username}:${nexus.password}@${nexus.host}/service/local/lucene/search?a=${getSearchTerms(versionInformation, scalaVersion)}"
    println("query = " + query)
    Try {
      versionsFromNexus(XML.load(new URL(query)))
        .filterNot (isEarlyRelease)
        .sortWith (comparator)
        .headOption.map(_.toString)
    }.recover{
      case e => e.printStackTrace(); None
    }
  }.toOption.flatten

  case class NexusCredentials(host:String, username:String, password:String)

  object NexusCredentials{
    def apply(credMap:Map[String, String]):NexusCredentials = NexusCredentials(
      credMap("host"),
      credMap("user"),
      credMap("password"))
  }


  private def findLocalNexusCreds(out:Logger):Try[NexusCredentials]= Try{
    val credsFile = System.getProperty("user.home") + "/.sbt/.credentials"
    out.info(s"[bobby] reading nexus credentials from $credsFile")

    val credMap = Source.fromFile(credsFile)
      .getLines().toSeq
      .map(_.split("="))
      .map { case Array(key, value) => key -> value}.toMap

    NexusCredentials(credMap)
  }

  implicit class OptionPimp[A](o:Option[A]){
    def toTry(ex:Exception):Try[A] = o.map(Success(_)).getOrElse(Failure(ex))
  }

  implicit class EitherPimp[L <: Throwable,R](e:Either[L,R]){
    def toTry:Try[R] = e.fold(Failure(_), Success(_))
  }

}
