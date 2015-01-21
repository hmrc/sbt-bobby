package uk.gov.hmrc

import java.net.URL

import sbt.Keys._
import sbt._
import uk.gov.hmrc.Core.Version

import scala.io.Source
import scala.util.Try
import scala.xml.{NodeSeq, XML}

object SbtBobbyPlugin extends AutoPlugin {

  object autoImport {
    lazy val policeDependencyVersions = taskKey[Unit]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
  }

  override def trigger = allRequirements


  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,
    autoImport.policeDependencyVersions := {
      streams.value.log.info(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
      val nexus = findLocalNexusCreds(streams.value.log) // TODO: populate with bobbyNexus config
      nexus.fold(streams.value.log.error("Unable to run bobby, no bobbyNexus provided")) {
        nexusRepo =>
          streams.value.log.info(s"[bobby] using nexus at '${nexusRepo.host}'")
          for ( module <- libraryDependencies.value) {
            latestRevision(module, scalaVersion.value, nexusRepo) match {
              case None => streams.value.log.info(s"Unable to get a latestRelease number for ${module.toString()}")
              case Some(latest) => {
                if (versionIsNewer(latest, module.revision))
                  streams.value.log.warn(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
              }
            }
          }
      }
    }
  )

  case class ShorteningState(currentVersion : String = "", decimalPoints : Int = 0, complete : Boolean = false)

  def versionIsNewer(isThisNewerThan : String, thisOne : String) : Boolean = {
    isThisNewerThan > thisOne
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
      Core.versionsFromNexus(XML.load(new URL(query)))
        .filterNot (Version.isEarlyRelease)
        .sortWith (Version.comparator)
        .headOption.map(_.toString)
    }.recover{
      case e => e.printStackTrace(); None
    }
  }.toOption.flatten

  case class NexusCredentials(host:String, username:String, password:String)


  private def findLocalNexusCreds(out:Logger):Option[NexusCredentials]= Try{
    val credsFile = System.getProperty("user.home") + "/.sbt/.credentials"
    out.info(s"[bobby] reading nexus credentials from $credsFile")

    val credMap = Source.fromFile(credsFile)
      .getLines().toSeq
      .map(_.split("="))
      .map { case Array(key, value) => key -> value}.toMap

    Some(NexusCredentials(
      credMap("host"),
      credMap("user"),
      credMap("password")))

  }.recover{
    case e => {
      out.error("[bobby] failed to read credentials due to " + e.getMessage)
      out.trace(e)
      None
    }
  }.toOption.flatten

}
