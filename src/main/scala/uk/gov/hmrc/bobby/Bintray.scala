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

import play.api.libs.json.Json
import sbt.ModuleID
import uk.gov.hmrc.bobby.conf.BintrayCredentials

import scala.io.Source
import scala.util.{Success, Try}

case class BintraySearchResult(latest_version:String)

object BintraySearchResult{
  implicit val format = Json.format[BintraySearchResult]
}

object Bintray {
  def apply(credentials: Option[BintrayCredentials]): Option[Bintray] = credentials.map(c => new Bintray {
    override val bintrayCred: BintrayCredentials = c
  })
}


trait Bintray extends RepoSearch{

  val bintrayCred:BintrayCredentials

  def latestVersion(json: String):String = {
    Json.parse(json).as[List[BintraySearchResult]].head.latest_version
  }

  def buildSearchUrl(versionInformation: ModuleID, scalaVersion: Option[String]): URL={
    import java.net.URLEncoder.encode
    new URL(s"https://${encode(bintrayCred.user, "UTF-8")}:${encode(bintrayCred.password, "UTF-8")}@bintray.com/api/v1/search/packages?subject=hmrc&repo=releases&name=${versionInformation.name}")
  }

  def query(url: URL): Try[Option[String]] = {
    Success(Some(latestVersion(Source.fromURL(url).mkString)))
  }

  override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[String]] = {
    query(buildSearchUrl(versionInformation, scalaVersion))
  }
}
