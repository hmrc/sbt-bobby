package uk.gov.hmrc

import java.net.URL

import sbt.Keys._
import sbt._

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
      val dependencies: Seq[ModuleID] = libraryDependencies.value
      val projectName = name.value
      streams.value.log.info(s"[bobby] is now interrogating the dependencies to in '$projectName''")
      val nexus = findLocalNexusCreds(streams.value.log) // TODO: populate with bobbyNexus config
      nexus.fold(streams.value.log.error("Unable to run bobby, no bobbyNexus provided")) {
        nexusRepo =>
          streams.value.log.info(s"[bobby] using nexus at '${nexusRepo.host}'")
          for ( module <- dependencies ) {
            latestRevision(module, scalaVersion.value, nexusRepo).fold(streams.value.log.info(s"Unable to get a latestRelease number for ${module.toString()}")) {
              latest =>
                if (versionIsNewer(latest, module.revision))
                  streams.value.log.warn(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '${latest}'")
            }
          }
      }
    }
  )

  case class ShorteningState(currentVersion : String = "", decimalPoints : Int = 0, complete : Boolean = false)

  def versionIsNewer(isThisNewerThan : String, thisOne : String) : Boolean = {
    isThisNewerThan > thisOne
  }

  // im sure there is a built in scala trick for this, couldn't find it though, so this will get us started...
  def shortenScalaVersion(scalaVersion : String) : String = {
    val maxDecimalPoints = 1
    scalaVersion.foldLeft[ShorteningState](ShorteningState()) {
      (ss : ShorteningState, c : Char) =>
        if (!ss.complete && ss.decimalPoints < 2) {
          val isDecimalPoint = c.equals('.')
          val decimalInc: Int = isDecimalPoint match { case true => 1 case false => 0 }
          val constructionComplete = isDecimalPoint && ss.decimalPoints == maxDecimalPoints
          val appendedVersion = constructionComplete match { case true => ss.currentVersion case false => ss.currentVersion + c }
          ShorteningState(appendedVersion, ss.decimalPoints + decimalInc, constructionComplete)
        } else {
          ss
        }
    }.currentVersion
  }

  private def getSearchTerms(versionInformation: ModuleID, scalaVersion : String) : String = {
    val shortenedScalaVersion = shortenScalaVersion(scalaVersion)
    s"${versionInformation.name}_$shortenedScalaVersion&&g=${versionInformation.organization}"
  }

  private def latestRevision(versionInformation: ModuleID, scalaVersion : String, nexus : NexusCredentials): Option[String] = {
    val query = s"https://${nexus.username}:${nexus.password}@${nexus.host}/service/local/lucene/search?a=${getSearchTerms(versionInformation, scalaVersion)}"
    Try {
      val nodes = XML.load(new URL(query)) \\ "latestRelease"
      nodes.headOption.map(_.text)
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
