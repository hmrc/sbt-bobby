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

import java.lang.Throwable
import java.net.URL
import java.net.URLEncoder._
import java.util.concurrent.TimeUnit

import play.api.libs.json.Json
import play.api.libs.ws.{DefaultWSClientConfig, WSResponse, WSAuthScheme}
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}
import sbt.ModuleID
import uk.gov.hmrc.bobby.conf.BintrayCredentials
import uk.gov.hmrc.bobby.domain.RepoSearch
import uk.gov.hmrc.bobby.domain.Version

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

case class BintraySearchResult(latest_version: String, name: String)

object BintraySearchResult {
  implicit val format = Json.format[BintraySearchResult]
}

object Bintray {
  def apply(credentials: Option[BintrayCredentials]): Option[Bintray] = credentials.map(c => new Bintray {
    override val bintrayCred: BintrayCredentials = c
  })
}


trait Bintray extends RepoSearch {

  val repoName = "Bintray"

  val bintrayCred: BintrayCredentials

  val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new DefaultWSClientConfig).build())

  def latestVersion(json: String, name: String): Option[Version] = {
    Json.parse(json).as[List[BintraySearchResult]]
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

    val call = ws
      .url(url)
      .withRequestTimeout(5000)
      .withAuth(
        encode(bintrayCred.user, "UTF-8"),
        encode(bintrayCred.password, "UTF-8"), WSAuthScheme.BASIC)
      .withHeaders("content-type" -> "application/json")
      .get()

    val result: WSResponse = Await.result(call, Duration.apply(5, TimeUnit.MINUTES))

    result.status match {
      case s if s >= 200 && s < 300 => Success(result.body)
      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when reading from Bintray. Got status ${result.status}: ${result.body}"))
    }
  }

  override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[Version]] = {
    query(buildSearchUrl(versionInformation, scalaVersion), versionInformation.name)
  }
}
