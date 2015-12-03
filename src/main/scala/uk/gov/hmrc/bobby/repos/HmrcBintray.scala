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

package uk.gov.hmrc.bobby.repos

import java.net.URL

import sbt.ModuleID
import uk.gov.hmrc.bobby.domain.{RepoSearch, Version}
import uk.gov.hmrc.bobby.{Helpers, Http}

import scala.util.{Failure, Try}
import scala.xml.XML

object HmrcBintray extends RepoSearch {

  val repoName = "Bintray"

  def latestVersion(xmlString: String): Option[Version] = {

    (XML.loadString(xmlString) \ "versioning" \ "latest")
      .headOption.map(_.text.trim)
      .map { Version.apply }
  }

  def buildSearchUrl(versionInformation: ModuleID, scalaVersion: Option[String]): URL = {
    val moduleNameSuffix = scalaVersion.map { sv => s"_$sv"}.getOrElse("")

    new URL(s"https://bintray.com/artifact/download/hmrc/releases/uk/gov/hmrc/${versionInformation.name}$moduleNameSuffix/maven-metadata.xml")
  }

  def query(url: URL): Try[Option[Version]] = {

    Http.get(url.toString).map { res =>
      latestVersion(res)
    }
  }

  override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
    import Helpers._

    versionInformation.organization match {

      case "uk.gov.hmrc" => {
        query(buildSearchUrl(versionInformation, scalaVersion)).flatMap{ ov =>
          ov.toTry(new Exception("didn't find version in Bintray"))
        }
      }

      case _ => Failure(new Exception("(non-hmrc)"))
    }
  }
}
