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

import java.util.concurrent.TimeUnit

import play.api.libs.ws.DefaultWSClientConfig
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}
import sbt.ModuleID
import uk.gov.hmrc.bobby.conf.NexusCredentials
import uk.gov.hmrc.bobby.domain.RepoSearch

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

object Nexus {
  def apply(credentials: Option[NexusCredentials]): Option[Nexus] = credentials.map(c => new Nexus {
    override val nexus: NexusCredentials = c
  })
}

trait Nexus extends RepoSearch{

  val repoName = "Nexus"

  import uk.gov.hmrc.bobby.domain.Version._
  import uk.gov.hmrc.bobby.domain._

  val nexus: NexusCredentials

  val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new DefaultWSClientConfig(connectionTimeout = Some(10000))).build())

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Option[String]]={
    query(nexus.buildSearchUrl(getSearchTerms(versionInformation, scalaVersion)))
  }


  def parseVersions(xml: NodeSeq): Seq[Version] = {
    val nodes = xml \ "data" \ "artifact" \ "version"
    nodes.map(n => Version(n.text))
  }

  private def query(url: String): Try[Option[String]] = Try {

    val resultF = ws.url(url).get().map { result =>
      parseVersions(result.xml)
        .filterNot(isSnapshot)
        .sortWith(comparator)
        .headOption.map(_.toString)
    }

    resultF.onFailure{ case e:Exception =>
      logger.warn(s"[bobby] failed to get dependency $url due to ${e.getMessage}")
    }

    Await.result(resultF, Duration(12, TimeUnit.SECONDS))
  }

  private def getSearchTerms(versionInformation: ModuleID, maybeScalaVersion: Option[String]): String = {
    maybeScalaVersion match {
      case Some(sv) => s"${versionInformation.name}_$maybeScalaVersion&g=${versionInformation.organization}"
      case None => s"${versionInformation.name}&g=${versionInformation.organization}"
    }
  }

}




