/*
 * Copyright 2018 HM Revenue & Customs
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

import sbt.{ConsoleLogger, ModuleID}
import uk.gov.hmrc.bobby.Helpers
import uk.gov.hmrc.bobby.conf.NexusCredentials
import uk.gov.hmrc.bobby.domain.RepoSearch

import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}

object Nexus {
  def apply(credentials: Option[NexusCredentials]): Option[Nexus] =
    credentials.map(c =>
      new Nexus {
        override val nexus: NexusCredentials = c
    })
}

trait Nexus extends RepoSearch {

  val repoName = "Nexus"

  import uk.gov.hmrc.bobby.domain.Version._
  import uk.gov.hmrc.bobby.domain._

  val logger = ConsoleLogger()

  val nexus: NexusCredentials

  def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
    import Helpers._

    versionInformation.organization match {

      case "uk.gov.hmrc" => Failure(new Exception("(hmrc-lib)"))

      case _ => {
        executeQuery(versionInformation, scalaVersion).flatMap { ov =>
          val nonScalaVersionResult: Try[Option[Version]] = (scalaVersion, ov) match {

            case (Some(_), None) => {
              executeQuery(versionInformation, None)
            }
            case _ => Success(ov)
          }

          nonScalaVersionResult.flatMap(_.toTry(new Exception("(see bintray)")))
        }
      }
    }
  }

  def executeQuery(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[Version]] =
    queryNexus(nexus.buildSearchUrl(getSearchTerms(versionInformation, scalaVersion)))

  def parseVersions(xml: NodeSeq): Seq[Version] = {
    val nodes = xml \ "data" \ "artifact" \ "version"
    nodes.map(n => Version(n.text))
  }

  private def queryNexus(url: String): Try[Option[Version]] = Try {
    parseVersions(XML.load(new URL(url)))
      .filterNot(isSnapshot)
      .sortWith(comparator)
      .headOption
  }

  def getSearchTerms(versionInformation: ModuleID, maybeScalaVersion: Option[String]): String =
    maybeScalaVersion match {
      case Some(sv) => s"${versionInformation.name}_$sv&g=${versionInformation.organization}"
      case None     => s"${versionInformation.name}&g=${versionInformation.organization}"
    }

}
