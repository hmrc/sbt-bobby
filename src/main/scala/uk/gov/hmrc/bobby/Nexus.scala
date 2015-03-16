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
package uk.gov.hmrc.bobby

import java.net.URL

import sbt.{ConsoleLogger, ModuleID}

import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}


trait Nexus {

  import uk.gov.hmrc.bobby.domain.Version._
  import uk.gov.hmrc.bobby.domain._

  val logger = ConsoleLogger()

  val nexus: NexusCredentials

  def findLatestRevision(versionInformation: ModuleID, scalaVersion: String): Option[String] = {
    queryNexus(nexus.buildSearchUrl(getSearchTerms(versionInformation, Some(scalaVersion)))) match {
      case Success(s) if s.isDefined => s
      case Success(s) => queryNexus(nexus.buildSearchUrl(getSearchTerms(versionInformation, None))).toOption.flatten
      case Failure(e) => logger.warn(s"Unable to query nexus: ${e.getClass.getName}: ${e.getMessage}"); None
    }
  }

  def versionsFromNexus(xml: NodeSeq): Seq[Version] = {
    val nodes = xml \ "data" \ "artifact" \ "version"
    nodes.map(n => Version(n.text))
  }

  private def queryNexus(url: String): Try[Option[String]] = Try {

    versionsFromNexus(XML.load(new URL(url)))
      .filterNot(isSnapshot)
      .sortWith(comparator)
      .headOption.map(_.toString)
  }

  def shortenScalaVersion(scalaVersion: String): String = {
    scalaVersion.split('.') match {
      case Array(major, minor, _*) => major + "." + minor
    }
  }

  private def getSearchTerms(versionInformation: ModuleID, maybeScalaVersion: Option[String]): String = {
    maybeScalaVersion match {
      case Some(sv) => s"${versionInformation.name}_${shortenScalaVersion(sv)}&g=${versionInformation.organization}"
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