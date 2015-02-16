package uk.gov.hmrc.bobby

import java.net.URL

import sbt.{Logger, ModuleID}

import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}



object Nexus{

  import uk.gov.hmrc.bobby.domain.Version._
  import uk.gov.hmrc.bobby.domain._

  def findLocalNexusCreds(out:Logger):Try[NexusCredentials]= Try{
    val credsFile = System.getProperty("user.home") + "/.sbt/.credentials"
    out.info(s"[bobby] reading nexus credentials from $credsFile")

    val credMap = Source.fromFile(credsFile)
      .getLines().toSeq
      .map(_.split("="))
      .map { case Array(key, value) => key -> value}.toMap

    NexusCredentials(credMap)
  }

  // TODO fail if nexus version is 2 or more major releases behind
  def checkDependency(module:ModuleID, latestNexusRevision:Option[String]): DependencyCheckResult ={

    latestNexusRevision match {
      case None => NotFoundInNexus
      case Some(latestNexus) if latestNexus > module.revision => NexusHasNewer(latestNexus)
      case Some(latestNexus) => OK
    }
  }
  //TODO test nexus connection and fail if we can't connect
  def findLatestRevision(versionInformation: ModuleID, scalaVersion : String, nexus : NexusCredentials): Option[String] = {
    queryNexus(nexus.buildSearchUrl(getSearchTerms(versionInformation, Some(scalaVersion)))) match {
      case Success(s) => s
      case Success(None) => queryNexus(nexus.buildSearchUrl(getSearchTerms(versionInformation, None))).toOption.flatten
      case Failure(e) => e.printStackTrace(); None
    }
  }

  def versionsFromNexus(xml:NodeSeq):Seq[Version] ={
    val nodes = xml \ "data" \ "artifact" \ "version"
    nodes.map(n => Version(n.text))
  }

  private def queryNexus(url:String):Try[Option[String]]= Try {

    versionsFromNexus(XML.load(new URL(url)))
      .filterNot (isEarlyRelease)
      .sortWith (comparator)
      .headOption.map(_.toString)
  }

  case class NexusCredentials(host:String, username:String, password:String){
    def buildSearchUrl(searchQuery:String) = s"https://${username}:${password}@${host}/service/local/lucene/search?a=$searchQuery"
  }

  object NexusCredentials{
    def apply(credMap:Map[String, String]):NexusCredentials = NexusCredentials(
      credMap("host"),
      credMap("user"),
      credMap("password"))
  }


  def shortenScalaVersion(scalaVersion : String):String = {
    scalaVersion.split('.') match {
      case Array(major, minor, _*) => major + "." + minor
    }
  }

  private def getSearchTerms(versionInformation: ModuleID, maybeScalaVersion : Option[String]) : String = {
    maybeScalaVersion match {
      case Some(sv) => s"${versionInformation.name}_${shortenScalaVersion(sv)}&g=${versionInformation.organization}"
      case None     => s"${versionInformation.name}&g=${versionInformation.organization}"
    }
  }

}
