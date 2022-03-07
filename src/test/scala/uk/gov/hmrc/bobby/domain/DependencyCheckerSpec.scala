/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import sbt.librarymanagement.ModuleID

class DependencyCheckerSpec extends AnyFlatSpec with Matchers {
  import uk.gov.hmrc.bobby.Util._

  val bv = BobbyValidator

  "The mandatory dependency checker" should "return success result if the version is not in a restricted range" in {
    val d = ModuleID("uk.gov.hmrc", "some-service", "2.0.0")
    val rule =
      BobbyRule(d.toDependency(), VersionRange("(,1.0.0]"), "testing", LocalDate.now().minusDays(1), List.empty)
    bv.calc(List(rule), d, "project") shouldBe BobbyOk
  }

  it should "return failed result if the version is in a restricted range" in {
    val d    = ModuleID("uk.gov.hmrc", "some-service", "0.1.0")
    val rule =
      BobbyRule(d.toDependency(), VersionRange("(,1.0.0]"), "testing", LocalDate.now().minusDays(1), List.empty)
    bv.calc(List(rule), d, "project") shouldBe BobbyViolation(rule)
  }

  it should "return warn result if the version is in a restricted range, and the result should be the highest precedence deprecation" in {
    val d = ModuleID("uk.gov.hmrc", "some-service", "3.1.0")
    val rule1 =
      BobbyRule(d.toDependency(), VersionRange("(,6.0.0]"), "testing 2", LocalDate.now().plusDays(2), List.empty)
    val rule2 =
      BobbyRule(d.toDependency(), VersionRange("(,5.0.0]"), "testing 1", LocalDate.now().plusDays(1), List.empty)
    val rule3 =
      BobbyRule(d.toDependency(), VersionRange("(,7.0.0]"), "testing 3", LocalDate.now().plusDays(3), List.empty)

    bv.calc(List(rule1, rule2, rule3), d, "project") shouldBe BobbyWarning(rule3)
  }

  it should "always return a violation over a warning if both rules apply" in {
    val d = ModuleID("uk.gov.hmrc", "some-service", "3.1.0")
    val rule1 =
      BobbyRule(d.toDependency(), VersionRange("(,6.0.0]"), "testing 2", LocalDate.now().plusDays(2), List.empty)
    val rule2 =
      BobbyRule(d.toDependency(), VersionRange("(,5.0.0]"), "testing 1", LocalDate.now().minusDays(1), List.empty)
    val rule3 =
      BobbyRule(d.toDependency(), VersionRange("(,7.0.0]"), "testing 3", LocalDate.now().plusDays(3), List.empty)

    bv.calc(List(rule1, rule2, rule3), d, "project") shouldBe BobbyViolation(rule2)
  }

  it should "return failed result if the version is in a restricted range of multiple exclude" in {
    val d = ModuleID("uk.gov.hmrc", "some-service", "1.1.0")
    val rule1 =
      BobbyRule(d.toDependency(), VersionRange("(,1.0.0]"), "testing", LocalDate.now().minusDays(1), List.empty)
    val rule2 =
      BobbyRule(d.toDependency(), VersionRange("[1.0.0,1.2.0]"), "testing", LocalDate.now().minusDays(1), List.empty)
    val rule3 =
      BobbyRule(d.toDependency(), VersionRange("[2.0.0,2.2.0]"), "testing", LocalDate.now().minusDays(1), List.empty)

    bv.calc(List(rule1, rule2, rule3), d, "project") shouldBe BobbyViolation(rule2)
  }

  it should "return warning if excludes are not applicable yet" in {
    val d                   = ModuleID("uk.gov.hmrc", "some-service", "0.1.0")
    val tomorrow: LocalDate = LocalDate.now().plusDays(1)
    val rule = BobbyRule(d.toDependency(), VersionRange("(,1.0.0]"), "testing", tomorrow, List.empty)
    bv.calc(List(rule), d, "project") shouldBe BobbyWarning(rule)
  }

  it should "return fail if exclude is applicable from today" in {
    val d                = ModuleID("uk.gov.hmrc", "some-service", "0.1.0")
    val today: LocalDate = LocalDate.now()
    val rule             = BobbyRule(d.toDependency(), VersionRange("(,1.0.0]"), "testing", today, List.empty)
    bv.calc(List(rule), d, "project") shouldBe BobbyViolation(rule)
  }

  it should "return failed result if the version is in both restricted warn and fail ranges" in {
    val d                        = ModuleID("uk.gov.hmrc", "some-service", "1.1.0")
    val validTomorrow: LocalDate = LocalDate.now().plusDays(1)
    val validToday: LocalDate    = LocalDate.now().minusDays(1)
    val rule1 = BobbyRule(d.toDependency(), VersionRange("[1.0.0,1.2.0]"), "testing1", validTomorrow, List.empty)
    val rule2 = BobbyRule(d.toDependency(), VersionRange("[1.0.0,2.2.0]"), "testing2", validToday, List.empty)

    bv.calc(List(rule1, rule2), d, "project") shouldBe BobbyViolation(rule2)
  }

  it should "filter non-relevant deprecated dependencies and return success" in {
    val d     = ModuleID("uk.gov.hmrc", "some-service", "1.1.0")
    val other = ModuleID("uk.gov.hmrc", "some-other-service", "1.1.0")
    val rules = (
      List(
        BobbyRule(other.toDependency(), VersionRange("(,1.0.0]"), "testing", LocalDate.now().minusDays(1), List.empty),
        BobbyRule(other.toDependency(), VersionRange("[1.0.0,1.2.0]"), "testing", LocalDate.now().minusDays(1), List.empty),
        BobbyRule(other.toDependency(), VersionRange("[2.0.0,2.2.0]"), "testing", LocalDate.now().minusDays(1), List.empty)
      )
    )

    bv.calc(rules, d, "project") shouldBe BobbyOk
  }

  it should "filter non-relevant deprecated dependencies and return fail when deprecated" in {
    val d     = ModuleID("uk.gov.hmrc", "some-service", "2.1.0")
    val other = ModuleID("uk.gov.hmrc", "some-other-service", "2.1.0")

    val rule1 = BobbyRule(other.toDependency(), VersionRange("(,3.0.0]"), "testing", LocalDate.now().minusDays(1), List.empty)
    val rule2 = BobbyRule(d.toDependency(), VersionRange("[2.0.0,2.2.0]"), "testing2", LocalDate.now().minusDays(1), List.empty)

    bv.calc(List(rule1, rule2), d, "project") shouldBe BobbyViolation(rule2)
  }

  it should "work when there is no deprecated dependencies" in {
    bv.calc(List.empty, ModuleID("org", "me", "1.2.3"), "project") shouldBe BobbyOk
  }

  it should "return failed result if the version has snapshot and [*-SNAPSHOT] range is set" in {
    val d = ModuleID("uk.gov.hmrc", "some-service", "0.1.0-SNAPSHOT")
    val rule = BobbyRule(d.toDependency(), VersionRange("[*-SNAPSHOT]"), "testing", LocalDate.now().minusDays(1), List.empty)
    bv.calc(List(rule), d, "project") shouldBe BobbyViolation(rule)
  }

  it should "return ok result if the version has no snapshot and [*-SNAPSHOT] range is set" in {
    val d = ModuleID("uk.gov.hmrc", "some-service", "0.1.0")
    val rule = BobbyRule(d.toDependency(), VersionRange("[*-SNAPSHOT]"), "testing", LocalDate.now().minusDays(1), List.empty)
    bv.calc(List(rule), d, "project") shouldBe BobbyOk
  }

  it should "support '*' wildcard in organisation and name" in {
    val rule = BobbyRule(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "testing", LocalDate.now().minusDays(1), List.empty)
    bv.calc(List(rule), ModuleID("uk.gov.hmrc", "some-service", "0.1.0-SNAPSHOT"), "project") shouldBe BobbyViolation(rule)
  }

  it should "support '*' wildcard in name only" in {
    val rule = BobbyRule(Dependency("uk.gov.hmrc", "*"), VersionRange("[*-SNAPSHOT]"), "testing", LocalDate.now().minusDays(1), List.empty)
    bv.calc(List(rule), ModuleID("uk.gov.hmrc", "some-service", "0.1.0-SNAPSHOT"), "project") shouldBe BobbyViolation(rule)
    bv.calc(List(rule), ModuleID("org.scalatest", "some-service", "0.1.0-SNAPSHOT"), "project") shouldBe BobbyOk
  }

  it should "exempt projects from rule matches if their names exist in the rule's exemption list" in {
    val rule1 =
      BobbyRule(
        Dependency("uk.gov.hmrc", "*"),
        VersionRange("(,99.99.99]"),
        "testing",
        LocalDate.now().minusDays(1),
        List("test-project-1", "test-project-2")
      )

    val rule2 =
      BobbyRule(
        Dependency("com.acme", "*"),
        VersionRange("(,99.99.99]"),
        "testing",
        LocalDate.now().plusDays(1),
        List("test-project-1")
      )

    val rules =
      List(rule1, rule2)

    bv.calc(rules, ModuleID("uk.gov.hmrc", "some-service", "1.0.0"), "test-project-x") shouldBe BobbyViolation(rule1)
    bv.calc(rules, ModuleID("com.acme", "some-service", "1.0.0"), "test-project-x") shouldBe BobbyWarning(rule2)

    bv.calc(rules, ModuleID("uk.gov.hmrc", "some-service", "1.0.0"), "test-project-1") shouldBe BobbyExemption(rule1)
    bv.calc(rules, ModuleID("com.acme", "some-service", "1.0.0"), "test-project-1") shouldBe BobbyExemption(rule2)
  }
}
