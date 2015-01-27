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
import uk.gov.hmrc.Core.Version

import scala.io.Source
import scala.util.Try
import scala.xml.{NodeSeq, XML}

object SbtBobbyPlugin extends AutoPlugin {

  import Core._
  import Version._

  object autoImport {
    lazy val checkNexusDependencyVersions = taskKey[Unit]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val checkMandatoryDependencyVersions = taskKey[Unit]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
    lazy val mandatoryUrl = settingKey[Option[URL]]("URL for mandatory dependencies")
  }
  
  override def trigger = allRequirements

  import autoImport._

  //TODO fail build by using SBT State object instead of horrible 'sys.error' calls
  //TODO de-stringify and use the Version object everywhere
  //TODO more more code into Core
  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,
    mandatoryUrl := Some(new File("../bobby/src/test/resources/mandatory-example.txt").toURI.toURL),
    checkMandatoryDependencyVersions := {
      if(mandatoryUrl.value.isEmpty) streams.value.log.warn(s"No setting for ${mandatoryUrl.key.label}, skipping mandatory check")

      mandatoryUrl.value.foreach { mandatoryUrl =>
        streams.value.log.info(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
        val mandatories: Map[OrganizationName, String] = getMandatoryVersions(Source.fromURL(mandatoryUrl).mkString)

        val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
          module -> getMandatoryResult(module, mandatories)
        }.toMap

        outputResult(streams.value, dependencyResults)
      }
    },

    checkNexusDependencyVersions := {
      streams.value.log.info(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
      val nexus = findLocalNexusCreds(streams.value.log)

      nexus.fold {
        streams.value.log.error("Didn't find Nexus credentials, cannot continue")
        sys.error("Didn't find Nexus credentials, cannot continue")
      } {
        nexusRepo => {
          streams.value.log.info(s"[bobby] using nexus at '${nexusRepo.host}'")

          val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module =>
            module -> getNexusResult(module, latestRevision(module, scalaVersion.value, nexusRepo))
          }.toMap

          outputResult(streams.value, dependencyResults)
        }
      }
    }
  )

  def outputResult(out:TaskStreams, dependencyResults: Map[ModuleID, DependencyCheckResult]) {
    dependencyResults.foreach { case(module, result) => result match {
      case MandatoryFail(latest) => out.log.error(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
      case NotFoundInNexus       => out.log.info(s"Unable to get a latestRelease number for '${module.toString()}'")
      case NexusHasNewer(latest) => out.log.warn(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
      case _ =>
    }}
    
    dependencyResults.values.find {
      _.fail
    }.fold(
        out.log.success("No invalid dependencies")
      ) { fail =>
          //sys.error("One or more dependency version check failed, see previous error.\nThis means you are using an out-of-date library which is not allowed in services deployed on the Tax Platform.")
          out.log.warn("One or more dependency version check failed, see previous error.\nThis means you are using an out-of-date library which is not allowed in services deployed on the Tax Platform.")
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
    s"${versionInformation.name}_$shortenedScalaVersion&&g=${versionInformation.organization}"
  }

  private def latestRevision(versionInformation: ModuleID, scalaVersion : String, nexus : NexusCredentials): Option[String] = {
    val query = s"https://${nexus.username}:${nexus.password}@${nexus.host}/service/local/lucene/search?a=${getSearchTerms(versionInformation, scalaVersion)}"
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


  private def findLocalNexusCreds(out:Logger):Option[NexusCredentials]= Try{
    val credsFile = System.getProperty("user.home") + "/.sbt/.credentials"
    out.info(s"[bobby] reading nexus credentials from $credsFile")

    val credMap = Source.fromFile(credsFile)
      .getLines().toSeq
      .map(_.split("="))
      .map { case Array(key, value) => key -> value}.toMap

    Some(NexusCredentials(credMap))

  }.recover{
    case e => {
      out.error("[bobby] failed to read credentials due to " + e.getMessage)
      out.trace(e)
      None
    }
  }.toOption.flatten

}
