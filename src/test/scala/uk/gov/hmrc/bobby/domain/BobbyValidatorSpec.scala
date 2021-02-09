/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sbt.ModuleID
import uk.gov.hmrc.bobby.Generators.depedendencyGen
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}
import uk.gov.hmrc.bobby.output.Flat

import scala.util.Random

class BobbyValidatorSpec extends AnyWordSpecLike with Matchers with ScalaCheckDrivenPropertyChecks {

  def deprecatedSoon(
    org: String,
    name: String,
    version: String): BobbyRule =
    BobbyRule(
      Dependency(org, name),
      VersionRange(version),
      "reason",
      LocalDate.now().plusWeeks(1))

  def deprecatedNow(
    org: String,
    name: String,
    version: String,
    reason: String                 = "reason",
    deadline: LocalDate            = LocalDate.now().minusWeeks(1)): BobbyRule =
    BobbyRule(Dependency(org, name), VersionRange(version), reason, deadline)

  def bobbyRules(rules: BobbyRule*): List[BobbyRule] = rules.toList

  "BobbyValidator.applyBobbyRules" should {
    "return error if a library is in the exclude range" in {
      val rules = bobbyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectLibs = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectLibs, rules)

      messages.head.level shouldBe ERROR
    }

    "not return error if a library is not in the exclude range" in {
      val rules = bobbyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "return error if a library is in the exclude range using wildcards for org, name and version number " in {
      val rules = bobbyRules(deprecatedNow("*", "*", "[*-SNAPSHOT]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)

      messages.head.level shouldBe ERROR

      Flat.renderMessage(messages.head)(4).plainText shouldBe "[*-SNAPSHOT]"
    }

    "not return error for valid libraries that don't include snapshots" in {
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val rules = bobbyRules(deprecatedNow("*", "*", "[*-SNAPSHOT]"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "return error if one of several dependencies is in the exclude range" in {
      val projectLibraries =
        Seq(
          ModuleID("uk.gov.hmrc", "auth", "3.2.1"),
          ModuleID("uk.gov.hmrc", "data-stream", "0.2.1"),
          ModuleID("uk.gov.hmrc", "data-stream-plugin", "0.2.1")
        )

      val rules = bobbyRules(
        deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"),
        deprecatedSoon("uk.gov.hmrc", "data-stream", "(,4.0.0]"),
        deprecatedSoon("uk.gov.hmrc", "data-stream-plugin", "(0.2.0)")
      )

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectLibraries, rules)
      messages.map(_.level) shouldBe List(ERROR, WARN, INFO)
    }

    "not return error for libraries in the exclude range but not applicable yet" in {
      val rules = bobbyRules(deprecatedSoon("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)
      messages.head.level shouldBe WARN
    }

    "not return error for mandatory libraries which are superseded" in {
      val rules = bobbyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "produce warning message for mandatory libraries which will be enforced in the future" in {
      val rules = bobbyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)
      messages.head.level shouldBe WARN
    }

    "produce error message for mandatory libraries which are currently enforced" in {
      val rules = bobbyRules(
        deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]", reason = "the reason", deadline = LocalDate.of(2000, 1, 1)))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies,rules)

      Flat.renderMessage(messages.head)(0).plainText shouldBe "ERROR"
      Flat.renderMessage(messages.head)(1).plainText shouldBe "uk.gov.hmrc.auth"
      Flat.renderMessage(messages.head)(2).plainText shouldBe ""
      Flat.renderMessage(messages.head)(5).plainText shouldBe "2000-01-01"
      Flat.renderMessage(messages.head)(6).plainText shouldBe "the reason"
    }

    "show a ERROR message for a library which is a bobby violation" in {
      val rules = bobbyRules(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)
      Flat.renderMessage(messages.head)(0).plainText shouldBe "ERROR"
    }

    "show a WARN message for a library which is a bobby warning" in {
      val rules = bobbyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, rules)

      messages.size shouldBe 1
      messages.head.level shouldBe WARN
    }

    "show an INFO message for a library which is not a violation or warning" in {
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, List.empty)

      messages.size shouldBe 1
      messages.head.level shouldBe INFO
    }

  }

  "BobbyValidator.calc" should {

    "give BobbyViolation if time is later than rule" in {
      forAll(depedendencyGen) { dep =>
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now())
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m) shouldBe BobbyViolation(rule)
      }
    }

    "give BobbyViolation if time is equal to rule" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m, now) shouldBe BobbyViolation(rule)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 1a: takes no upper bound first)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now)
        val rule2 = BobbyRule(dep, VersionRange("[0.1.0,)"), "some reason", now)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule, rule2), m, now) shouldBe BobbyViolation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 1b: takes highest upper bound)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("(,0.1.0]"), "some reason", now)
        val rule2 = BobbyRule(dep, VersionRange("(,0.3.0]"), "some reason", now)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule, rule2), m, now) shouldBe BobbyViolation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 2: takes inclusive upper bound)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("(,0.1.0)"), "some reason", now)
        val rule2 = BobbyRule(dep, VersionRange("(,0.1.0]"), "some reason", now)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule, rule2), m, now) shouldBe BobbyViolation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 3: takes most recent)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("(0.1.0]"), "some reason", now.minusDays(2))
        val rule2 = BobbyRule(dep, VersionRange("(0.1.0]"), "some reason", now.minusDays(1))
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule, rule2), m, now) shouldBe BobbyViolation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (all priorities, duplicating test from Catalogue)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val orderedList = List(
          BobbyRule(dep, VersionRange("[1.0.0,)"), "some reason", now),
          BobbyRule(dep, VersionRange("(,99.99.99)"), "some reason", now),
          BobbyRule(dep, VersionRange("(,2.1.0)"), "some reason", now),
          BobbyRule(dep, VersionRange("(,1.2.0]"), "some reason", now),
          BobbyRule(dep, VersionRange("(,1.2.0)"), "some reason", now),
          BobbyRule(dep, VersionRange("(,1.2.0)"), "some reason", now.minusDays(1))
        )
        Random.shuffle(orderedList).sorted shouldBe orderedList
      }
    }

    "give BobbyWarning if time is before rule" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m, now.minusDays(1)) shouldBe BobbyWarning(rule)
      }
    }

    "give BobbyOk if version is not in outlawed range" in {
      forAll(depedendencyGen) { dep =>
        val d = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now())
        val m = ModuleID(d.dependency.organisation, d.dependency.name, "0.1.1")
        BobbyValidator.calc(List(d), m) shouldBe BobbyOk
      }
    }
  }
}
