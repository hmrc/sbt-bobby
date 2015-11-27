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

import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys._
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain.{AggregateRepoSearch, RepoSearch}

object Repositories {


  def buildAggregateRepositories(reposValue:Seq[Repo], checkForLatest: Boolean):Option[AggregateRepoSearch] ={
    if (!checkForLatest) None else Some {
      new AggregateRepoSearch() {
        val repoName = "aggregate"
        override val repos = buildRepos(reposValue).flatten
      }
    }
  }


  def buildRepos(repos:Seq[Repo]):Seq[Option[RepoSearch]]={
    repos map {
      case BobbyKeys.Bintray => Bintray(Configuration.bintrayCredetials)
      case BobbyKeys.Nexus   => Nexus(Configuration.nexusCredetials)
      case BobbyKeys.Maven   => Some(Maven)
    }
  }

}
