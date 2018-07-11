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

package uk.gov.hmrc.bobby.domain

import sbt.{ConsoleLogger, ModuleID}
import uk.gov.hmrc.bobby.Helpers

import scala.util.{Failure, Success, Try}
import Helpers._

trait AggregateRepoSearch extends RepoSearch{

  val log = ConsoleLogger()

  def repos:Seq[RepoSearch]

  def findBestMatch(seq:Seq[Try[Version]]):Try[Version]={
    seq.collect { case Success(v) => v }
      .sortWith((a, b) => a.compareTo(b) < 0)
      .reverse
      .headOption
      .toTry(new Exception("(Not found)"))
  }

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Version]={
    val latestVersions: Seq[Try[Version]] = repos.map { r =>
      val latestVersion = r.findLatestRevision(versionInformation, scalaVersion)
      latestVersion match {
        case Success(v) => log.debug(s"[bobby] found ${versionInformation.organization}.${versionInformation.name}.${v.toString} in ${r.repoName}")
        case Failure(e) => log.debug(s"[bobby] Didn't find ${versionInformation.organization}.${versionInformation.name} in ${r.repoName}, reason: ${e.getMessage}")
      }

      latestVersion
    }

    findBestMatch(latestVersions)
  }

}
