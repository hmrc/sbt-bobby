/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sbt.ModuleID
import uk.gov.hmrc.bobby.Generators.{depedendencyGen, dependencyTypeGen}
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}
import uk.gov.hmrc.bobby.output.Flat

class BobbyValidatorSpec extends AnyWordSpecLike with Matchers with ScalaCheckDrivenPropertyChecks {

  def deprecatedSoon(
    org: String,
    name: String,
    version: String,
    dependencyType: DependencyType = Library): BobbyRule =
    BobbyRule(
      Dependency(org, name),
      VersionRange(version),
      "reason",
      new LocalDate().plusWeeks(1),
      dependencyType)

  def deprecatedNow(
    org: String,
    name: String,
    version: String,
    reason: String                 = "reason",
    deadline: LocalDate            = new LocalDate().minusWeeks(1),
    dependencyType: DependencyType = Library): BobbyRule =
    BobbyRule(Dependency(org, name), VersionRange(version), reason, deadline, dependencyType)

  def boddyRules(rules: BobbyRule*) =
    BobbyRules(rules.toList)

  "BobbyValidator.applyBobbyRules" should {
    "return error if a library is in the exclude range" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectLibs = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectLibs, Seq.empty, Map.empty, rules)

      messages.head.level shouldBe ERROR
    }

    "return error if a plugin is in the exclude range" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth-plugin", "[3.2.1]", dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth-plugin", "3.2.1"))
      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)
      messages.head.level shouldBe ERROR
    }

    "not return error if a library is not in the exclude range" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "not return error if a plugin is not in the exclude range" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]", dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "return error if a library is in the exclude range using wildcards for org, name and version number " in {
      val rules = boddyRules(deprecatedNow("*", "*", "[*-SNAPSHOT]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)

      messages.head.level shouldBe ERROR

      Flat.renderMessage(messages.head)(4).plainText shouldBe "[*-SNAPSHOT]"
    }

    "return error if a plugin is in the exclude range using wildcards for org, name and version number " in {
      val rules = boddyRules(deprecatedNow("*", "*", "[*-SNAPSHOT]", dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)

      messages.head.level shouldBe ERROR
      Flat.renderMessage(messages.head)(4).plainText shouldBe "[*-SNAPSHOT]"
    }

    "not return error for valid libraries that don't include snapshots" in {
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val rules = boddyRules(deprecatedNow("*", "*", "[*-SNAPSHOT]"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "not return error for valid plugins that don't include snapshots" in {
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val rules = boddyRules(deprecatedNow("*", "*", "[*-SNAPSHOT]", dependencyType = Plugin))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "return error if one of several dependencies is in the exclude range" in {
      val projectLibraries =
        Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"), ModuleID("uk.gov.hmrc", "data-stream", "0.2.1"))

      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "data-stream-plugin", "0.2.1"))

      val rules = boddyRules(
        deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"),
        deprecatedSoon("uk.gov.hmrc", "data-stream", "(,4.0.0]"),
        deprecatedSoon("uk.gov.hmrc", "data-stream-plugin", "(0.2.0)", dependencyType = Plugin)
      )

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectLibraries, projectPlugins, Map.empty, rules)
      messages.map(_.level) shouldBe List(ERROR, WARN, INFO)
    }

    "not return error for libraries in the exclude range but not applicable yet" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)
      messages.head.level shouldBe WARN
    }

    "not return error for plugins in the exclude range but not applicable yet" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "[3.2.1]", dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)
      messages.head.level shouldBe WARN
    }

    "not return error for mandatory libraries which are superseded" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "not return error for mandatory plugins which are superseded" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]", dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)
      messages.head.checked.result shouldBe BobbyOk
    }

    "produce warning message for mandatory libraries which will be enforced in the future" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)
      messages.head.level shouldBe WARN
    }

    "produce warning message for mandatory plugins which will be enforced in the future" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]", dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)
      messages.head.level shouldBe WARN
    }

    "produce error message for mandatory libraries which are currently been enforced" in {
      val rules = boddyRules(
        deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]", reason = "the reason", deadline = new LocalDate(2000, 1, 1)))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, Map.empty, rules)

      Flat.renderMessage(messages.head)(0).plainText shouldBe "ERROR"
      Flat.renderMessage(messages.head)(1).plainText shouldBe "uk.gov.hmrc.auth"
      Flat.renderMessage(messages.head)(2).plainText shouldBe ""
      Flat.renderMessage(messages.head)(3).plainText shouldBe "3.2.0"
      Flat.renderMessage(messages.head)(6).plainText shouldBe "2000-01-01"
      Flat.renderMessage(messages.head)(7).plainText shouldBe "the reason"
    }

    "produce error message for mandatory plugins which are currently been enforced" in {
      val rules = boddyRules(
        deprecatedNow(
          "uk.gov.hmrc",
          "auth",
          "(,4.0.0]",
          reason = "the reason",
          deadline = new LocalDate(2000, 1, 1),
          dependencyType = Plugin))
      val projectPlugins = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, Seq.empty, projectPlugins, Map.empty, rules)

      val pluginMessage = messages.head
      Flat.renderMessage(pluginMessage)(0).plainText shouldBe "ERROR"
      Flat.renderMessage(pluginMessage)(1).plainText shouldBe "uk.gov.hmrc.auth"
      Flat.renderMessage(pluginMessage)(2).plainText shouldBe ""
      Flat.renderMessage(pluginMessage)(3).plainText shouldBe "3.2.0"
      Flat.renderMessage(pluginMessage)(6).plainText shouldBe "2000-01-01"
      Flat.renderMessage(pluginMessage)(7).plainText shouldBe "the reason"
    }

    "show a ERROR message for a library which has a newer version in a repository AND is a mandatory upgrade now" in {
      val rules = boddyRules(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Some(Version("4.3.0")))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)
      Flat.renderMessage(messages.head)(0).plainText shouldBe "ERROR"
    }

    "show a WARN message for a library which has a newer version in a repository AND is a mandatory upgrade soon" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Some(Version("4.3.0")))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)

      messages.size shouldBe 1
      messages.head.level shouldBe WARN
      Flat.renderMessage(messages.head).map(_.plainText) should contain("3.2.1")
      Flat.renderMessage(messages.head).map(_.plainText) should contain("4.3.0")
    }

    "show an INFO message for a library which has a newer version in a repository" in {
      val rules = BobbyRules.EMPTY
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Some(Version("3.3.0")))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)

      messages.size shouldBe 1
      messages.head.level shouldBe INFO
      Flat.renderMessage(messages.head).map(_.plainText) should contain("3.2.1")
      Flat.renderMessage(messages.head).map(_.plainText) should contain("3.3.0")
    }

    "not show a message if a library is up-to-date" in {
      val rules = BobbyRules.EMPTY
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Some(Version("3.2.1")))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)

      messages.head.checked.result shouldBe BobbyOk
    }

    "show an INFO message for a library for which the latest nexus revision is unknown and show '?' in the results table" in {
      val rules = BobbyRules.EMPTY
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> None)

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)

      messages.head.level shouldBe INFO
      Flat.renderMessage(messages.head).map(_.plainText) should contain("3.2.1")
      Flat.renderMessage(messages.head).map(_.plainText) should contain("?")
    }

    "show an WARN message for a library which will be rules soon AND has a newer version in a repository" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Some(Version("3.8.0")))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)

      messages.head.level shouldBe WARN
      Flat.renderMessage(messages.head).map(_.plainText) should contain("(,4.0.0]")
      Flat.renderMessage(messages.head).map(_.plainText) should contain("3.2.1")
      Flat.renderMessage(messages.head).map(_.plainText) should contain("3.8.0")
      Flat.renderMessage(messages.head).map(_.plainText) should not contain "4.0.0"
    }

    "not show an earlier version of a mandatory library if the latest was not found in a repository" in {
      val rules = boddyRules(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
      val projectDependencies = Seq(ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
      val latestVersionMap = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Option(Version("1.0.0")))

      val messages = BobbyValidator.applyBobbyRules(Map.empty, projectDependencies, Seq.empty, latestVersionMap, rules)

      Flat.renderMessage(messages.head).map(_.plainText) should not contain "1.0.0"
      Flat.renderMessage(messages.head).map(_.plainText) should not contain "3.1.0"
    }

  }

  "BobbyValidator.calc" should {

    "give BobbyViolation if time is later than rule" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now(), t)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m) shouldBe BobbyViolation(rule)
      }
    }

    "give BobbyViolation if time is equal to rule" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, t)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m, now) shouldBe BobbyViolation(rule)
      }
    }

    "give BobbyViolation when there are multiple matching rules" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now.minusDays(1), t)
        val rule2 = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now.minusDays(2), t)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule, rule2), m, now) shouldBe BobbyViolation(rule2)
      }
    }

    "give BobbyWarning if time is before rule" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val now = LocalDate.now()
        val rule = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, t)
        val m = ModuleID(rule.dependency.organisation, rule.dependency.name, "0.1.0")
        BobbyValidator.calc(List(rule), m, now.minusDays(1)) shouldBe BobbyWarning(rule)
      }
    }

    "give BobbyOk if version is not in outlawed range" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val d = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now(), t)
        val m = ModuleID(d.dependency.organisation, d.dependency.name, "0.1.1")
        BobbyValidator.calc(List(d), m) shouldBe BobbyOk
      }
    }
  }
}
