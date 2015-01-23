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
    lazy val checkDependencyVersions = taskKey[Unit]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
  }
  
  override def trigger = allRequirements

  //TODO fail build by using SBT State object instead of horrible 'sys.error' calls
  //TODO de-stringify and use the Version object everywhere
  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,
    autoImport.checkDependencyVersions := {
      streams.value.log.info(s"[bobby] is now interrogating the dependencies to in '${name.value}''")
      val nexus = findLocalNexusCreds(streams.value.log)
      val mandatories: Map[OrganizationName, String] = getMandatoryVersions(new URL("file:///Users/ck/hmrc/hmrc-development-environment/hmrc/bobby/src/test/resources/mandatory-example.txt"))

      nexus.fold {
        streams.value.log.error("Didn't find Nexus credentials, cannot continue")
        sys.error("Didn't find Nexus credentials, cannot continue")
      } {
        nexusRepo => {
          streams.value.log.info(s"[bobby] using nexus at '${nexusRepo.host}'")

          val dependencyResults: Map[ModuleID, DependencyCheckResult] = libraryDependencies.value.map { module => //for ( module <- libraryDependencies.value) {
            module -> getResult(module, latestRevision(module, scalaVersion.value, nexusRepo), mandatories)
          }.toMap

          dependencyResults.foreach { case(module, result) => result match {
            case NotFound              => streams.value.log.info(s"Unable to get a latestRelease number for '${module.toString()}'")
            case NexusHasNewer(latest) => streams.value.log.warn(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
            case MandatoryFail(latest) => streams.value.log.error(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '$latest'")
            case _ =>
          }
          }

          dependencyResults.values.find { _.fail }.fold (
            streams.value.log.success("No invalid dependencies")
          ){ fail =>
            sys.error("One or more dependency version check failed, see previous error.\nThis means you are using an out-of-date library which is not allowed in services deployed on the Tax Platfrom.")
          }
        }
      }
    }
  )

  sealed trait DependencyCheckResult {
    def fail:Boolean
  }
  trait Fail extends DependencyCheckResult { val fail = true}
  trait Pass extends DependencyCheckResult { val fail = false}

  case class MandatoryFail(latest:String) extends DependencyCheckResult with Fail
  case class NexusHasNewer(latest:String) extends DependencyCheckResult with Pass
  object NotFound extends DependencyCheckResult with Pass
  object OK extends DependencyCheckResult with Pass


  // TODO fail if nexus version is 2 or more major releases behind
  // TODO 'merge' the mandatory and nexus result more effecivley
  def getResult(module:ModuleID, latestNexusRevision:Option[String], mandatories: Map[OrganizationName, String]): DependencyCheckResult ={
    latestNexusRevision match {
      case None => NotFound
      case Some(latestNexus) => {
        val nexusCheck = if (latestNexus > module.revision) {
          NexusHasNewer(latestNexus)
        } else { OK }

        val mandatoryCheck = mandatories.get(OrganizationName(module.organization, module.name)).map { mandatory =>
          if(mandatory > latestNexus){
            MandatoryFail(mandatory)
          } else {
            OK
          }
        }.getOrElse(NotFound)
        
        if (mandatoryCheck.fail){
          mandatoryCheck
        } else {
          nexusCheck
        }
      }
    }
  }

  case class ShorteningState(currentVersion : String = "", decimalPoints : Int = 0, complete : Boolean = false)

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
