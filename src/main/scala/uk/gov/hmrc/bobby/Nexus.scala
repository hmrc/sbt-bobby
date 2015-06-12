/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bobby

import java.net.URL

import sbt.{ModuleID, ConsoleLogger}
import uk.gov.hmrc.bobby.domain.Version
import uk.gov.hmrc.bobby.domain.Version._

import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}

object MavenSearch extends RepoSearch{

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Option[String]]={
    query(buildSearchUrl(getSearchTerms(versionInformation, scalaVersion)))
  }


  def buildSearchUrl(searchQuery: String) = s"http://search.maven.org/solrsearch/select?q=$searchQuery%22&core=gav&rows=20&wt=xml"

  private def getSearchTerms(versionInformation: ModuleID, maybeScalaVersion: Option[String]): String = {
    val scalaSuffix = maybeScalaVersion.map(s => "_" + s) getOrElse ""
    s"g:%22${versionInformation.organization}%22%20AND%20a:%22${versionInformation.name}$scalaSuffix"
  }


  def parseVersions(xml: NodeSeq): Seq[Version] = {
    (xml \ "result" \ "doc" \ "str" )
      .filter(n => (n \ "@name").text.trim == "v")
      .map(v => Version(v.text.trim))
  }

  private def query(url: String): Try[Option[String]] = Try {
    parseVersions(XML.load(new URL(url)))
      .filterNot(isSnapshot)
      .sortWith(comparator)
      .headOption.map(_.toString)
  }

}

trait RepoSearch{

  def shortenScalaVersion(scalaVersion: String): String = {
    scalaVersion.split('.') match {
      case Array(major, minor, _*) => major + "." + minor
    }
  }

  def findLatestRevision(versionInformation: ModuleID, scalaVersion: Option[String]): Option[String] = {
    search(versionInformation, scalaVersion.map{ shortenScalaVersion }) match {
      case Success(s) if s.isDefined => s
      case Success(s) => search(versionInformation, None).toOption.flatten
      case Failure(e) => e.printStackTrace(); None //logger.warn(s"Unable to query nexus: ${e.getClass.getName}: ${e.getMessage}"); None
    }
  }

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Option[String]]
}

trait Nexus extends RepoSearch{

  import uk.gov.hmrc.bobby.domain.Version._
  import uk.gov.hmrc.bobby.domain._

  val logger = ConsoleLogger()

  val nexus: NexusCredentials

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Option[String]]={
    query(nexus.buildSearchUrl(getSearchTerms(versionInformation, scalaVersion)))
  }


  def parseVersions(xml: NodeSeq): Seq[Version] = {
    val nodes = xml \ "data" \ "artifact" \ "version"
    nodes.map(n => Version(n.text))
  }

  private def query(url: String): Try[Option[String]] = Try {

    parseVersions(XML.load(new URL(url)))
      .filterNot(isSnapshot)
      .sortWith(comparator)
      .headOption.map(_.toString)
  }

  private def getSearchTerms(versionInformation: ModuleID, maybeScalaVersion: Option[String]): String = {
    maybeScalaVersion match {
      case Some(sv) => s"${versionInformation.name}_$maybeScalaVersion&g=${versionInformation.organization}"
      case None => s"${versionInformation.name}&g=${versionInformation.organization}"
    }
  }

}

case class NexusCredentials(host: String, username: String, password: String) {

  import java.net.URLEncoder.encode

  def buildSearchUrl(searchQuery: String) = s"https://${encode(username, "UTF-8")}:${encode(password, "UTF-8")}@${host}/service/local/lucene/search?a=$searchQuery"
}

object Nexus {
  def apply(credentials: Option[NexusCredentials]): Option[Nexus] = credentials.map(c => new Nexus {
    override val nexus: NexusCredentials = c
  })
}
