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

import java.net.{HttpURLConnection, URL}

import org.apache.commons.codec.binary.Base64
import sbt.ModuleID
import uk.gov.hmrc.bobby.Helpers
import uk.gov.hmrc.bobby.NativeJsonHelpers._
import uk.gov.hmrc.bobby.conf.BintrayCredentials
import uk.gov.hmrc.bobby.domain.{RepoSearch, Version}

import scala.io.Source
import scala.util.parsing.json.JSON
import scala.util.{Success, Failure, Try}

case class BintraySearchResult(latest_version: String, name: String)

object Bintray {
  def apply(credentials: Option[BintrayCredentials]): Option[Bintray] = credentials.map(c => new Bintray {
    override val bintrayCred: BintrayCredentials = c
  })
}

trait Bintray extends RepoSearch {

  val repoName = "Bintray"

  val bintrayCred: BintrayCredentials

  def latestVersion(jsonString: String, name: String): Option[Version] = {

    val results = for {
      Some(L(list)) <- List(JSON.parseFull(jsonString))
      MS(map) <- list
      latest_version <- map.get("latest_version")
      name <- map.get("name")
    } yield  BintraySearchResult(latest_version, name)

    results
      .find(r => r.name == name)
      .map(v => Version(v.latest_version))
  }

  def buildSearchUrl(versionInformation: ModuleID, scalaVersion: Option[String]): URL = {

    new URL(s"https://bintray.com/api/v1/search/packages?subject=hmrc&repo=releases&name=${versionInformation.name}")
  }

  def query(url: URL, name: String): Try[Option[Version]] = {

    get(url.toString).map { res =>
      latestVersion(res, name)
    }
  }

  def get[A](url: String): Try[String] = {

    val con = new URL(url).openConnection().asInstanceOf[HttpURLConnection]

    con.setRequestMethod("GET")
    con.setRequestProperty("content-type", "application/json")
    val userpass = bintrayCred.user + ":" + bintrayCred.password
    val basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()))
    con.setRequestProperty ("Authorization", basicAuth)

    con.connect()

    val resultStatus =  con.getResponseCode

    val resultBody = Source.fromInputStream(con.getInputStream).mkString

    resultStatus match {
      case s if s >= 200 && s < 300 => Success(resultBody)
      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when reading from Bintray. Got status ${resultStatus}: ${resultBody}"))
    }
  }

  override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
    import Helpers._

    query(buildSearchUrl(versionInformation, scalaVersion), versionInformation.name).flatMap{ ov =>
      ov.toTry(new Exception("didn't find version in Bintray"))
    }
  }
}
