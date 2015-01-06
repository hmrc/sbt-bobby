package uk.gov.hmrc

import sbt.Keys._
import sbt._

import scala.io.Source
import scala.xml.{NodeSeq, XML}

object SbtBobbyPlugin extends AutoPlugin {

  object autoImport {
    lazy val policeDependencyVersions = taskKey[Unit]("Check if each dependency is the newest and warn/fail if required, configured in '~/.sbt/global.sbt'")
  }

  lazy val customKey1 = SettingKey[Seq[(String, String)]]("mandatoryReleases") // TODO: This need to point to a list of mandatory versions
  lazy val bobbyNexus = SettingKey[String]("bobbyNexus")  // TODO: this needs to point to a configured nexus

  override def trigger = allRequirements
  override lazy val projectSettings = Seq(
    parallelExecution in GlobalScope := true,
    autoImport.policeDependencyVersions := {
      val dependencies: Seq[ModuleID] = libraryDependencies.value
      //print("foobar: " + customKey1.value.length)
      val projectName = name.value
      val r: SettingKey[String] = bobbyNexus
      val x: Def.Setting[String] = r.:=("foo")
      streams.value.log.info(s"[bobby] is now interrogating the dependencies to in [$projectName] using nexus '${x.toString()}r'")
      streams.value.log.info("--------------------------------------------------------------")
      val nexus = None // TODO: populate with bobbyNexus config
      nexus.fold(streams.value.log.error("Unable to run bobby, no bobbyNexus provided")) {
        nexusRepo =>
          for ( module <- dependencies ) {
            latestRevision(module, scalaVersion.value, "").fold(streams.value.log.info(s"Unable to get a latestRelease number for ${module.toString()}")) {
              latest =>
                if (versionIsNewer(latest, module.revision))
                  streams.value.log.info(s"Your version of ${module.name} is using '${module.revision}' out of date, consider upgrading to '${latest}'")
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

  private def latestRevision(versionInformation: ModuleID, scalaVersion : String, nexus : String): Option[String] = {
    val query = s"${nexus}search?a=${getSearchTerms(versionInformation, scalaVersion)}"
    val html = Source.fromURL(query)
    val s = html.mkString
    val xml = XML.loadString(s)
    val nodes: NodeSeq = xml \\ "latestRelease"
    val first: Option[scala.xml.Node] = nodes.headOption
    first.fold[Option[String]](None) {
      item =>
        Some(item.text)
    }
  }

}