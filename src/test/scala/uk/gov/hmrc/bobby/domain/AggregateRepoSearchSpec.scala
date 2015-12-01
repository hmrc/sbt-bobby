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

import org.scalatest.{TryValues, FlatSpec, Matchers}
import sbt.ModuleID

import scala.util.{Failure, Success, Try}


class AggregateRepoSearchSpec extends FlatSpec with Matchers with TryValues {

  val timeRepo = new RepoSearch {
    override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
      if (versionInformation.name == "time")
        Success(Version("3.2.1"))
      else
        Failure(new Exception("error"))
    }

    override def repoName: String = "timeRepo"
  }

  val timeRepo2 = new RepoSearch {
    override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
      if (versionInformation.name == "time")
        Success(Version("4.0.0"))
      else
        Failure(new Exception("error"))
    }

    override def repoName: String = "timeRepo"
  }

  val domainRepo = new RepoSearch {
    override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Version] = {
      if (versionInformation.name == "domain")
        Success(Version("3.0.0"))
      else
        Failure(new Exception("error"))
    }

    override def repoName: String = "domainRepo"

  }

  "AggregateRepoSearch" should "find the most recent version" in {

      val aggregateSearch = new AggregateRepoSearch {
        override def repoName: String = "test"

        override def repos: Seq[RepoSearch] = Seq(timeRepo2, timeRepo)
      }

      aggregateSearch.search(new ModuleID("uk.gov.hmrc", "time", "3.2.1"), None) shouldBe Success(Version("4.0.0"))
    }

  "AggregateRepoSearch" should  "look in two repositories to find a dependency" in {

      val aggregateSearch = new AggregateRepoSearch {
        override def repoName: String = "test"

        override def repos: Seq[RepoSearch] = Seq(timeRepo, domainRepo)
      }


      aggregateSearch.search(new ModuleID("uk.gov.hmrc", "time", "3.2.1"), None) shouldBe Success(Version("3.2.1"))
      aggregateSearch.search(new ModuleID("uk.gov.hmrc", "domain", "3.0.0"), None) shouldBe Success(Version("3.0.0"))
      aggregateSearch.search(new ModuleID("uk.gov.hmrc", "email", "1.2.1"), None).isFailure shouldBe true
    }

}
