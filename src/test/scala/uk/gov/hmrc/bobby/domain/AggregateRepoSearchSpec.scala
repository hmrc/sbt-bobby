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

import org.scalatest.{FlatSpec, Matchers}
import sbt.ModuleID
import uk.gov.hmrc.bobby.RepoSearch

import scala.util.{Success, Try}


class AggregateRepoSearchSpec extends FlatSpec with Matchers {

  "AggregateRepoSearch" should
    "look in two repositories to find a dependency" in {

    val timeRepo = new RepoSearch{
      override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[String]] = {
        if(versionInformation.name == "time")
          Success(Some("3.2.1"))
        else
          Success(None)
      }
    }
    val domainRepo = new RepoSearch{
      override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[String]] = {
        if(versionInformation.name == "domain")
          Success(Some("3.0.0"))
        else
          Success(None)
      }
    }

    val aggregateSearch = new AggregateRepoSearch {
      override def repos: Seq[RepoSearch] = Seq(timeRepo, domainRepo)
    }

    aggregateSearch.search(new ModuleID("uk.gov.hmrc", "time", "3.2.1"), None) shouldBe Success(Some("3.2.1"))
    aggregateSearch.search(new ModuleID("uk.gov.hmrc", "domain", "3.0.0"), None) shouldBe Success(Some("3.0.0"))
    aggregateSearch.search(new ModuleID("uk.gov.hmrc", "email", "1.2.1"), None) shouldBe Success(None)
  }
}
