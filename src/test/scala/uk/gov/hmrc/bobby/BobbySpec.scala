package uk.gov.hmrc.bobby

import org.joda.time.LocalDate
import org.scalatest.{Matchers, FlatSpec}
import sbt.{State, ModuleID}
import uk.gov.hmrc.bobby.domain.{VersionRange, Dependency, DeprecatedDependency}

class BobbySpec extends FlatSpec with Matchers {

  case class BobbyUnderTest(excludes: Seq[DeprecatedDependency]) extends Bobby {
    override val checker: DependencyChecker = DependencyCheckerUnderTest(excludes)
    override val nexus: Option[Nexus] = None
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

}
