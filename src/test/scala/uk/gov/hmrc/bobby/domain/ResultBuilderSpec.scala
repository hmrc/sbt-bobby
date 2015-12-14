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

import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}
import sbt.ModuleID
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}

import scala.util.{Failure, Success}

class ResultBuilderSpec extends FlatSpec with Matchers {

  def deprecatedSoon(org: String, name: String, version: String, dependencyType: DependencyType = Library): DeprecatedDependency = {
    DeprecatedDependency(Dependency(org, name), VersionRange(version), "reason", new LocalDate().plusWeeks(1), dependencyType)
  }

  def deprecatedNow(org: String, name: String, version: String, reason: String = "reason", deadline: LocalDate = new LocalDate().minusWeeks(1), dependencyType: DependencyType = Library): DeprecatedDependency = {
    DeprecatedDependency(Dependency(org, name), VersionRange(version), reason, deadline, dependencyType)
  }

  def dependencies(deps: DeprecatedDependency*) = {
    DeprecatedDependencies(deps.toList)
  }


  it should "return error if a library is in the exclude range" in {
    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
    val projectLibs = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

    val messages = ResultBuilder.calculate(projectLibs, Seq.empty, None, deprecated)

    messages.head.level shouldBe ERROR
  }

  it should "return error if a plugin is in the exclude range" in {
    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth-plugin", "[3.2.1]", dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth-plugin", "3.2.1"))
    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)
    messages.head.level shouldBe ERROR
  }

  it should "not return error if a library is not in the exclude range" in {
    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)
    messages shouldBe 'empty
  }

  it should "not return error if a plugin is not in the exclude range" in {
    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]", dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)
    messages shouldBe 'empty
  }

  it should "return error if a library is in the exclude range using wildcards for org, name and version number " in {

    val deprecated = dependencies(deprecatedNow("*", "*", "[*-SNAPSHOT]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)

    messages.head.level shouldBe ERROR
    messages.head.shortTabularOutput(3) shouldBe "[*-SNAPSHOT]"
  }

  it should "return error if a plugin is in the exclude range using wildcards for org, name and version number " in {

    val deprecated = dependencies(deprecatedNow("*", "*", "[*-SNAPSHOT]", dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)

    messages.head.level shouldBe ERROR
    messages.head.shortTabularOutput(3) shouldBe "[*-SNAPSHOT]"
  }

  it should "not return error for valid libraries that don't include snapshots" in {

    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val deprecated = dependencies(deprecatedNow("*", "*", "[*-SNAPSHOT]"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)
    messages shouldBe 'empty

  }

  it should "not return error for valid plugins that don't include snapshots" in {

    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val deprecated = dependencies(deprecatedNow("*", "*", "[*-SNAPSHOT]", dependencyType = Plugin))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)
    messages shouldBe 'empty

  }


  it should "return error if one of several dependencies is in the exclude range" in {

    val projectLibraries = Seq(
      new ModuleID("uk.gov.hmrc", "auth", "3.2.1"),
      new ModuleID("uk.gov.hmrc", "data-stream", "0.2.1"))

    val projectPlugins = Seq(
      new ModuleID("uk.gov.hmrc", "data-stream-plugin", "0.2.1"))


    val deprecated = dependencies(
      deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"),
      deprecatedSoon("uk.gov.hmrc", "data-stream", "(,4.0.0]"),
      deprecatedSoon("uk.gov.hmrc", "data-stream-plugin", "(0.2.0)", dependencyType = Plugin)
    )

    val messages = ResultBuilder.calculate(projectLibraries, projectPlugins, None, deprecated)
    messages.map(_.level).toSet shouldBe Set(WARN, ERROR)
  }

  it should "not return error for libraries in the exclude range but not applicable yet" in {

    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "[3.2.1]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)
    messages.head.level shouldBe WARN
  }

  it should "not return error for plugins in the exclude range but not applicable yet" in {

    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "[3.2.1]", dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)
    messages.head.level shouldBe WARN
  }

  it should "not return error for mandatory libraries which are superseded" in {

    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)
    messages shouldBe 'empty
  }

  it should "not return error for mandatory plugins which are superseded" in {

    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "[3.2.1]", dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.2"))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)
    messages shouldBe 'empty
  }


  it should "produce warning message for mandatory libraries which will be enforced in the future" in {

    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)
    messages.head.level shouldBe WARN
  }

  it should "produce warning message for mandatory plugins which will be enforced in the future" in {

    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]", dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)
    messages.head.level shouldBe WARN
  }

  it should "produce error message for mandatory libraries which are currently been enforced" in {

    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]", reason = "the reason", deadline = new LocalDate(2000, 1, 1)))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, None, deprecated)

    messages.head.longTabularOutput(0) shouldBe "ERROR"
    messages.head.longTabularOutput(1) shouldBe "uk.gov.hmrc.auth"
    messages.head.longTabularOutput(2) shouldBe "3.2.0"
    messages.head.longTabularOutput(5) shouldBe "2000-01-01"
    messages.head.longTabularOutput(6) shouldBe "the reason"
  }

  it should "produce error message for mandatory plugins which are currently been enforced" in {

    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]", reason = "the reason", deadline = new LocalDate(2000, 1, 1), dependencyType = Plugin))
    val projectPlugins = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

    val messages = ResultBuilder.calculate(Seq.empty, projectPlugins, None, deprecated)

    val pluginMessage = messages.head
    pluginMessage.longTabularOutput(0) shouldBe "ERROR"
    pluginMessage.longTabularOutput(1) shouldBe "uk.gov.hmrc.auth"
    pluginMessage.longTabularOutput(2) shouldBe "3.2.0"
    pluginMessage.longTabularOutput(5) shouldBe "2000-01-01"
    pluginMessage.longTabularOutput(6) shouldBe "the reason"
  }


  it should "show a ERROR message for a library which has a newer version in a repository AND is a mandatory upgrade now" in {
    val deprecated = dependencies(deprecatedNow("uk.gov.hmrc", "auth", "(,4.0.0]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Success(Version("4.3.0")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)
    messages.head.longTabularOutput(0) shouldBe "ERROR"
  }

  it should "show a WARN message for a library which has a newer version in a repository AND is a mandatory upgrade soon" in {
    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Success(Version("4.3.0")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)

    messages.size shouldBe 1
    messages.head.level shouldBe WARN
    messages.head.shortTabularOutput should contain("3.2.1")
    messages.head.shortTabularOutput should contain("4.3.0")
  }

  it should "show an INFO message for a library which has a newer version in a repository" in {
    val deprecated = DeprecatedDependencies.EMPTY
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Success(Version("3.3.0")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)

    messages.size shouldBe 1
    messages.head.level shouldBe INFO
    messages.head.shortTabularOutput should contain("3.2.1")
    messages.head.shortTabularOutput should contain("3.3.0")
  }

  it should "not show a message if a library is up-to-date" in {
    val deprecated = DeprecatedDependencies.EMPTY
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Success(Version("3.2.1")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)

    messages shouldBe 'empty
  }

  it should "show an INFO message for a library for which the latest nexus revision is unknown and show 'not-found' in the results table" in {
    val deprecated = DeprecatedDependencies.EMPTY
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Failure(new Exception("not-found")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)

    messages.head.level shouldBe INFO
    messages.head.shortTabularOutput should contain("3.2.1")
    messages.head.shortTabularOutput should contain("not-found")
  }

  it should "show an WARN message for a library which will be deprecated soon AND has a newer version in a repository" in {
    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Success(Version("3.8.0")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)

    messages.head.level shouldBe WARN
    messages.head.shortTabularOutput should contain("(,4.0.0]")
    messages.head.shortTabularOutput should contain("3.2.1")
    messages.head.shortTabularOutput should contain("3.8.0")
    messages.head.shortTabularOutput should not contain "4.0.0"
  }

  it should "not show an eariler version of a mandatory library if the latest was not found in a repository" in {
    val deprecated = dependencies(deprecatedSoon("uk.gov.hmrc", "auth", "(,4.0.0]"))
    val projectDependencies = Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1"))
    val repoDependencies = Map(new ModuleID("uk.gov.hmrc", "auth", "3.2.1") -> Success(Version("1.0.0")))

    val messages = ResultBuilder.calculate(projectDependencies, Seq.empty, Some(repoDependencies), deprecated)

    messages.head.shortTabularOutput should not contain "1.0.0"
    messages.head.shortTabularOutput should not contain "3.1.0"
  }
}
