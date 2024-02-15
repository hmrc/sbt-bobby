/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sbt.ModuleID
import uk.gov.hmrc.bobby.Generators.depedendencyGen
import uk.gov.hmrc.bobby.output.Flat

import scala.util.Random

class BobbyValidatorSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  val scope               = "compile"
  val internalModuleNodes = Seq.empty
  val projectName         = "project"

  def deprecatedSoon(
    org    : String,
    name   : String,
    version: String
  ): BobbyRule =
    BobbyRule(
      Dependency(org, name),
      VersionRange(version),
      "reason",
      LocalDate.now().plusWeeks(1),
      Set.empty
    )

  def deprecatedNow(
    org     : String,
    name    : String,
    version : String,
    reason  : String    = "reason",
    deadline: LocalDate = LocalDate.now().minusWeeks(1)
  ): BobbyRule =
    BobbyRule(
      Dependency(org, name),
      VersionRange(version),
      reason,
      deadline,
      Set.empty
    )

  def toGraph(dependencies: ModuleID*): String =
    s"""
    |digraph "dependency-graph" {
    |graph[rankdir="LR"]
    |edge [
    |    arrowtail="none"
    |]
    |"uk.gov.hmrc:service:0.1.0"[label=<uk.gov.hmrc<BR/><B>service</B><BR/>0.1.0> style=""]
    ${dependencies.map { m =>
      s"""|"${m.organization}:${m.name}_2.12:${m.revision}"[label=<${m.organization}<BR/><B>${m.name}_2.12</B><BR/>${m.revision}> style=""]"""
    }.mkString("\n")}
    ${dependencies.map { m =>
      s"""|"uk.gov.hmrc:service:0.1.0" -> "${m.organization}:${m.name}_2.12:${m.revision}""""
    }.mkString("\n")}
    """.stripMargin

  "BobbyValidator.validate" should {
    "return error if a library is in the exclude range" in {
      val bobbyRules  = Seq(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)
      result.hasViolations shouldBe true
    }

    "not return error if a library is not in the exclude range" in {
      val bobbyRules  = Seq(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.hasNoIssues shouldBe true
      result.allMessages.head.result shouldBe BobbyResult.Ok
    }

    "return error if a library is in the exclude range using wildcards for org, name and version number " in {
      val bobbyRules  = Seq(deprecatedNow("*", "*", "[*-SNAPSHOT]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      Flat.renderMessage(result.violations.head)(4).plainText shouldBe "[*-SNAPSHOT]"
    }

    "not return error for valid libraries that don't include snapshots" in {
      val bobbyRules  = Seq(deprecatedNow("*", "*", "[*-SNAPSHOT]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.allMessages.head.result shouldBe BobbyResult.Ok
    }

    "return error if one of several dependencies is in the exclude range" in {
      val bobbyRules = Seq(
        deprecatedNow("uk.gov.hmrc" , "auth"              , "(,4.0.0]"),
        deprecatedSoon("uk.gov.hmrc", "data-stream"       , "(,4.0.0]"),
        deprecatedSoon("uk.gov.hmrc", "data-stream-plugin", "(0.2.0)")
      )

      val graphString = toGraph(
          ModuleID("uk.gov.hmrc", "auth"              , "3.2.1"),
          ModuleID("uk.gov.hmrc", "data-stream"       , "0.2.1"),
          ModuleID("uk.gov.hmrc", "data-stream-plugin", "0.2.1")
        )

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.hasViolations shouldBe true
      result.hasWarnings shouldBe true
      result.allMessages.map(_.level) shouldBe List(MessageLevel.ERROR, MessageLevel.WARN, MessageLevel.INFO)
    }

    "not return error for libraries in the exclude range but not applicable yet" in {
      val bobbyRules  = Seq(deprecatedSoon("uk.gov.hmrc", "auth", "[3.2.1]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.allMessages.head.level shouldBe MessageLevel.WARN
      result.warnings shouldBe result.allMessages
    }

    "not return error for mandatory libraries which are superseded" in {
      val bobbyRules  = Seq(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)
      result.allMessages.head.result shouldBe BobbyResult.Ok
    }

    "produce warning message for mandatory libraries which will be enforced in the future" in {
      val bobbyRules  = Seq(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.hasWarnings shouldBe true
      result.warnings shouldBe result.allMessages
    }

    "produce error message for mandatory libraries which are currently enforced" in {
      val bobbyRules  = Seq(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]", reason = "the reason", deadline = LocalDate.of(2000, 1, 1)))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.violations shouldBe result.allMessages

      Flat.renderMessage(result.allMessages.head)(0).plainText shouldBe "ERROR"
      Flat.renderMessage(result.allMessages.head)(1).plainText shouldBe "uk.gov.hmrc:auth"
      Flat.renderMessage(result.allMessages.head)(2).plainText shouldBe ""
      Flat.renderMessage(result.allMessages.head)(5).plainText shouldBe "2000-01-01"
      Flat.renderMessage(result.allMessages.head)(6).plainText shouldBe "the reason"
    }

    "show a ERROR message for a library which is a bobby violation" in {
      val bobbyRules  = Seq(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.violations shouldBe result.allMessages
      Flat.renderMessage(result.allMessages.head)(0).plainText shouldBe "ERROR"
    }

    "show a WARN message for a library which is a bobby warning" in {
      val bobbyRules  = Seq(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.warnings.size shouldBe 1
      result.warnings shouldBe result.allMessages
    }

    "show an INFO message for a library which is not a violation or warning" in {
      val bobbyRules  = Seq.empty
      val graphString = toGraph(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.validate(graphString, scope, bobbyRules, internalModuleNodes, projectName)
      val result   = BobbyValidationResult(messages)

      result.allMessages.size shouldBe 1
    }
  }

  "BobbyValidator.calc" should {
    "give BobbyViolation if time is later than rule" in {
      forAll(depedendencyGen) { dep =>
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now(), Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(Seq(rule), m, "project") shouldBe BobbyResult.Violation(rule)
      }
    }

    "give BobbyViolation if time is equal to rule" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m, "project", now) shouldBe BobbyResult.Violation(rule)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 1a: takes no upper bound first)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, Set.empty)
        val rule2 = BobbyRule(dep, VersionRange("[0.1.0,)"), "some reason", now, Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(Seq(rule, rule2), m, "project", now) shouldBe BobbyResult.Violation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 1b: takes highest upper bound)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("(,0.1.0]"), "some reason", now, Set.empty)
        val rule2 = BobbyRule(dep, VersionRange("(,0.3.0]"), "some reason", now, Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(Seq(rule, rule2), m, "project", now) shouldBe BobbyResult.Violation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 2: takes inclusive upper bound)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("(,0.1.0)"), "some reason", now, Set.empty)
        val rule2 = BobbyRule(dep, VersionRange("(,0.1.0]"), "some reason", now, Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(Seq(rule, rule2), m, "project", now) shouldBe BobbyResult.Violation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (Priority 3: takes most recent)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("(0.1.0]"), "some reason", now.minusDays(2), Set.empty)
        val rule2 = BobbyRule(dep, VersionRange("(0.1.0]"), "some reason", now.minusDays(1), Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(Seq(rule, rule2), m, "project", now) shouldBe BobbyResult.Violation(rule2)
      }
    }

    "resolves correct rule when there are multiple matching rules (all priorities, duplicating test from Catalogue)" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val orderedList = Seq(
          BobbyRule(dep, VersionRange("[1.0.0,)"), "some reason", now, Set.empty),
          BobbyRule(dep, VersionRange("(,99.99.99)"), "some reason", now, Set.empty),
          BobbyRule(dep, VersionRange("(,2.1.0)"), "some reason", now, Set.empty),
          BobbyRule(dep, VersionRange("(,1.2.0]"), "some reason", now, Set.empty),
          BobbyRule(dep, VersionRange("(,1.2.0)"), "some reason", now, Set.empty),
          BobbyRule(dep, VersionRange("(,1.2.0)"), "some reason", now.minusDays(1), Set.empty)
        )
        Random.shuffle(orderedList).sorted shouldBe orderedList
      }
    }

    "give BobbyWarning if time is before rule" in {
      forAll(depedendencyGen) { dep =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, Set.empty)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(Seq(rule), m, "project", now.minusDays(1)) shouldBe BobbyResult.Warning(rule)
      }
    }

    "give BobbyOk if version is not in outlawed range" in {
      forAll(depedendencyGen) { dep =>
        val d = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now(), Set.empty)
        val m = ModuleID(d.dependency.organisation, d.dependency.name, "0.1.1")
        BobbyValidator.calc(Seq(d), m, "project") shouldBe BobbyResult.Ok
      }
    }
  }
}
