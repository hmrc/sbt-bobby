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
import uk.gov.hmrc.bobby.LogLevels.{ERROR, WARN, INFO}
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{TextOutingFileWriter, JsonOutingFileWriter}

import scala.util.{Success, Try}
import MessageBuilder._

class BobbySpec extends FlatSpec with Matchers {

  case class BobbyUnderTest(excludes: Seq[DeprecatedDependency]) extends Bobby {
    override val checker: DependencyChecker = DependencyCheckerUnderTest(excludes)
    override val repoSearch: RepoSearch = new RepoSearch {
      override def search(versionInformation: ModuleID, scalaVersion: Option[String]): Try[Option[Version]] = Success(None)
      override def repoName: String = ???
    }
    override val jsonOutputFileWriter: JsonOutingFileWriter = JsonOutingFileWriter
    override val textOutputFileWriter: TextOutingFileWriter = TextOutingFileWriter
  }

  private val failing = new LocalDate().minusDays(1)
  private val warning = new LocalDate().plusDays(2)

  private val snapshots = VersionRange("[*-SNAPSHOT]")
  private val upTo_4_0_0 = VersionRange("(,4.0.0]")

  private val anyProject = Dependency("*", "*")
  private val auth = Dependency("uk.gov.hmrc", "auth")
  private val datastream = Dependency("uk.gov.hmrc", "data-stream")

  private val failAnySnapshot = DeprecatedDependency(anyProject, snapshots, "reason", failing)
  private val warnAnySnapshot = DeprecatedDependency(anyProject, snapshots, "reason", warning)

  def aBobby(scope: LocalDate, project: Dependency, versions: VersionRange): BobbyUnderTest = {
    BobbyUnderTest(Seq(DeprecatedDependency(project, versions, "reason", scope)))
  }

  "Bobby" should "fail the build if a dependency is in the exclude range" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(Dependency("*", "*"), VersionRange("[*-SNAPSHOT]"), "reason", new LocalDate())))

    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT")), "2.11", false) shouldBe false
  }

  it should "not fail the build for valid dependencies" in {

    val bobby = aBobby(failing, anyProject, snapshots)

    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1")), "2.11", false) shouldBe true
  }

  it should "fail the build if one of several dependencies is in the exclude range" in {

    val bobby = BobbyUnderTest(Seq(
      DeprecatedDependency(datastream, snapshots, "reason", failing), // fail
      DeprecatedDependency(auth, snapshots, "reason", warning)   // warn
    ))

    bobby.areDependenciesValid(Seq(
            new ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT"),
            new ModuleID("uk.gov.hmrc", "data-stream", "0.2.1-SNAPSHOT")
          ),
      "2.11",
      false) shouldBe false
  }

  it should "not fail the build for valid dependencies, concrete version, disallow snapshot" in {
    val bobby = aBobby(failing, anyProject, snapshots)
    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1")), "2.11", false) shouldBe true
  }

  it should "not fail the build for dependencies in the exclude range but not applicable yet" in {

    val bobby = aBobby(warning, anyProject, snapshots)
    bobby.areDependenciesValid(Seq(new ModuleID("uk.gov.hmrc", "auth", "3.2.1")), "2.11", false) shouldBe true
  }

  it should "info for dependencies the latest nexus revision is unknown" in {

    val bobby = aBobby(warning, anyProject, snapshots)

    val results = bobby.calculateNexusResults(Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1", None) -> None))

    results.size shouldBe 1
    val message = results.head
    message.level shouldBe LogLevels.INFO
    message.message shouldBe "Unable to get a latestRelease number for 'uk.gov.hmrc:auth:3.2.1'"
  }

  it should "info for dependencies for which the latest revision is greater" in {

    val bobby = aBobby(warning, anyProject, snapshots)

    val latestRevisions = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.1", None) -> Some(Version("3.2.2")))

    val results = bobby.calculateNexusResults(latestRevisions)

    results.size shouldBe 1
    val message = results.head
    message.level shouldBe INFO
    message.message shouldBe "'uk.gov.hmrc.auth 3.2.1' is not the most recent version, consider upgrading to '3.2.2'"
  }

  it should "not fail the build for mandatory dependencies which will be enforced in the future" in {

    val bobby = aBobby(warning, auth, upTo_4_0_0)

    val latestRevisions = Map(ModuleID("uk.gov.hmrc", "auth", "3.2.0", None) -> Some(Version("3.2.2")))

    bobby.noErrorsExist(bobby.checkMandatoryDependencies(latestRevisions)) shouldBe true
  }

  it should "not fail the build for mandatory dependencies which are superseded" in {

    val bobby = aBobby(failing, auth, upTo_4_0_0)

    val latestRevisions = Map(ModuleID("uk.gov.hmrc", "auth", "5.0.0", None) -> Some(Version("5.0.0")))

    bobby.noErrorsExist(bobby.checkMandatoryDependencies(latestRevisions)) shouldBe true
  }

  it should "error for mandatory dependencies which are been enforced" in {

    val bobby = aBobby(failing, auth, upTo_4_0_0)

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.0", None)
    val latestRevisions = Map{moduleId -> Some(Version("3.2.2"))}

    bobby.noErrorsExist(bobby.checkMandatoryDependencies(latestRevisions)) shouldBe false
  }

  it should "produce warning message for mandatory dependencies which will be enforced in the future" in {

    val bobby = aBobby(warning, auth, upTo_4_0_0)

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc", "auth", "3.2.0", None)
    val latestRevisions = Map{moduleId -> Some(Version("3.2.2"))}

    val results = bobby.checkMandatoryDependencies(latestRevisions)

    results.size shouldBe 1
    val warn = results.head
    warn.level shouldBe WARN
    warn.message should include ("uk.gov.hmrc.auth 3.2.0 is deprecated")
    warn.message should include ("to version 3.2.2")
  }

  it should "produce error message for mandatory dependencies which are currently been enforced" in {

    val bobby = BobbyUnderTest(Seq(DeprecatedDependency(auth, upTo_4_0_0,"reason", new LocalDate().minusDays(2))))

    val moduleId: ModuleID = ModuleID("uk.gov.hmrc","auth", "3.2.0", None)
    val latestRevisions = Map{moduleId -> Some(Version("3.2.2"))}

    val results = bobby.checkMandatoryDependencies(latestRevisions)

    results.size shouldBe 1
    val error = results.head
    error.level shouldBe ERROR
    error.message should include ("uk.gov.hmrc.auth 3.2.0 is deprecated." )
  }

  it should "prepare dependencies by removing any on a blacklist" in {
    val auth = ModuleID("uk.gov.hmrc","auth", "3.2.0")

    val mods = Seq(
      auth,
      ModuleID("com.typesafe.play", "play-ws", "6.2.0", None)
    )

    val blacklisted: Set[String] = Set("com.typesafe.play")

    BobbyUnderTest(Seq()).prepareDependencies(mods, blacklisted) shouldBe Seq(auth)
  }

  it should "order messages correctly" in {

    val messages = Seq(
      makeMessage(INFO, "info message"),
      makeMessage(ERROR, "error message"),
      makeMessage(WARN, "warn message"))

    messages.sorted(Message.MessageOrdering).map(_.level) shouldBe Seq(ERROR, WARN, INFO)
  }
}
