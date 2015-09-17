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

package uk.gov.hmrc.bobby.domain

import sbt.{Level, ModuleID}
import uk.gov.hmrc.bobby.domain.RepoSearch

import scala.util.{Success, Try}

trait AggregateRepoSearch extends RepoSearch{

  def repos:Seq[RepoSearch]

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Option[String]]=Try{
    repos.foldLeft(None:Option[String]){ (agg, repo) =>
      agg.orElse{
        val start = System.currentTimeMillis()
        val revision: Option[String] = repo.findLatestRevision(versionInformation, scalaVersion)
        val end = System.currentTimeMillis() - start
        if(revision.isEmpty){
          logger.info(s"[bobby] [trace] Didn't find ${versionInformation} in ${repo.repoName}, op took $end ms")
        } else {
          logger.info(s"[bobby] [trace] Found ${versionInformation} in ${repo.repoName}, op took $end ms")
        }
        revision
      }
    }
  }
}
