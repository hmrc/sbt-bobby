/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.bobby.repos

import java.net.URL

import sbt.ModuleID
import uk.gov.hmrc.bobby.domain.{RepoSearch, Version}

import scala.util.Try
import scala.xml.{NodeSeq, XML}
import uk.gov.hmrc.bobby.Helpers._

//TODO retry without scala version for java libraries
object Maven extends RepoSearch{
  
  import Version._

  val repoName = "Maven"

  def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
    query(buildSearchUrl(getSearchTerms(versionInformation, scalaVersion))).flatMap{ ov =>
      ov.toTry(new Exception("(try Bintray)"))
    }
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

  private def query(url: String): Try[Option[Version]] = Try {
    parseVersions(XML.load(new URL(url)))
      .filterNot(isSnapshot)
      .sortWith(comparator)
      .headOption
  }

}
