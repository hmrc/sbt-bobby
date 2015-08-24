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

import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}
import sbt.ModuleID
import uk.gov.hmrc.bobby.domain.{Dependency, DeprecatedDependency, VersionRange}

import scala.util.{Success, Try}

class BobbySpec extends FlatSpec with Matchers {

  case class BobbyUnderTest(excludes: Seq[DeprecatedDependency]) extends Bobby {
    override val checker: DependencyChecker = DependencyCheckerUnderTest(excludes)
    override val repoSearch: RepoSearch = new RepoSearch {
      override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[String]] = Success(None)
    }
  }

  "Bobby" should "fail the build if a dependency is in the exclude range" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "reason", new LocalDate().minusDays(1))))

    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT")), "2.11") shouldBe false
  }

  it should "not fail the build for valid dependencies" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "reason", new LocalDate())))

    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1")), "2.11") shouldBe true
  }

  it should "not fail the build for dependencies in the exclude range but not applicable yet" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "reason", new LocalDate().plusDays(2))))

    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1")), "2.11") shouldBe true
  }

  it should "warn for dependencies the latest nexus revision is unknown" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "reason", new LocalDate().plusDays(2))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.1", None)

    val latestRevisions: Map[ModuleID, Option[String]] = Map{moduleId -> None}

    val results = bobby.calculateNexusResults(latestRevisions, false)

    results.size shouldBe 1
    results.head shouldBe "[bobby] Unable to get a latestRelease number for 'uk.gov.hmrc:auth:3.2.1'"
  }

  it should "warn for dependencies for which the latest revision is greater" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "reason", new LocalDate().plusDays(2))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.1", None)

    val latestRevisions: Map[ModuleID, Option[String]] = Map{moduleId -> Some("3.2.2")}

    val results = bobby.calculateNexusResults(latestRevisions, false)

    results.size shouldBe 1
    results.head shouldBe "[bobby] 'auth 3.2.1' is out of date, consider upgrading to '3.2.2'"
  }

  it should "not fail the build for mandatory dependencies which will be enforced in the future" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("uk.gov.hmrc", "auth"), VersionRange("(,4.0.0]"), "reason", new LocalDate().plusDays(2))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.0", None)
    val latestRevisions: Map[ModuleID, Option[String]] = Map{moduleId -> Some("3.2.2")}

    bobby.doMandatoryCheck(latestRevisions) shouldBe true
  }

  it should "error for mandatory dependencies which are been enforced" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("uk.gov.hmrc", "auth"), VersionRange("(,4.0.0]"), "reason", new LocalDate().minusDays(7))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.0", None)
    val latestRevisions: Map[ModuleID, Option[String]] = Map{moduleId -> Some("3.2.2")}

    bobby.doMandatoryCheck(latestRevisions) shouldBe false
  }

  it should "produce warning message for mandatory dependencies which will be enforced in the future" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("uk.gov.hmrc", "auth"), VersionRange("(,4.0.0]"), "reason", new LocalDate().plusDays(2))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.0", None)
    val latestRevisions: Map[ModuleID, Option[String]] = Map{moduleId -> Some("3.2.2")}

    val results: List[(String, String)] = bobby.logMandatoryCheckResults(latestRevisions)

    results.size shouldBe 1
    val warning: (String, String) = results.head
    warning._1 shouldBe "WARN"
    warning._2 should include ("auth 3.2.0' is deprecated!")
    warning._2 should include ("Please consider upgrading to '3.2.2'")
  }

  it should "produce error message for mandatory dependencies which are currently been enforced" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("uk.gov.hmrc","auth"), VersionRange("(,4.0.0]"),"reason", new LocalDate().minusDays(2))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc","auth", "3.2.0", None)
    val latestRevisions: Map[ModuleID, Option[String]] = Map{moduleId -> Some("3.2.2")}

    val results: List[(String, String)] = bobby.logMandatoryCheckResults(latestRevisions)

    results.size shouldBe 1
    val error: (String, String) = results.head
    error._1 shouldBe "ERROR"

    val errorMessage: String = error._2
    errorMessage should include ("The module 'auth auth 3.2.0' is deprecated." )
  }

}
