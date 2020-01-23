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
import org.scalatest.flatspec.AnyFlatSpec

class DependencyCheckerSpec extends AnyFlatSpec with Matchers {

  val dc = DependencyChecker

  "The mandatory dependency checker" should "return success result if the version is not in a restricted range" in {
    val d = Dependency("uk.gov.hmrc", "some-service")
    val deps: List[DeprecatedDependency] =
      List(DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1), Library))
    dc.isDependencyValid(deps)(
      d,
      Version("2.0.0")) shouldBe OK
  }

  it should "return failed result if the version is in a restricted range" in {
    val d    = Dependency("uk.gov.hmrc", "some-service")
    val deps = List(DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1), Library))
    dc.isDependencyValid(deps)(d, Version("0.1.0")) shouldBe MandatoryFail(
      DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1), Library))
  }

  it should "return warn result if the version is in when two restricted ranges, and the result should be the soonest deprecation" in {

    val d = Dependency("uk.gov.hmrc", "some-service")
    val deps = (
      List(
        DeprecatedDependency(d, VersionRange("(,6.0.0]"), "testing 2", new LocalDate().plusDays(2), Library),
        DeprecatedDependency(d, VersionRange("(,5.0.0]"), "testing 1", new LocalDate().plusDays(1), Library),
        DeprecatedDependency(d, VersionRange("(,7.0.0]"), "testing 3", new LocalDate().plusDays(3), Library)
      )
    )

    dc.isDependencyValid(deps)(d, Version("3.1.0")) shouldBe MandatoryWarn(
      DeprecatedDependency(d, VersionRange("(,5.0.0]"), "testing 1", new LocalDate().plusDays(1), Library))
  }

  it should "return failed result if the version is in a restricted range of multiple exclude" in {

    val d = Dependency("uk.gov.hmrc", "some-service")
    val deps = (
      List(
        DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1), Library),
        DeprecatedDependency(d, VersionRange("[1.0.0,1.2.0]"), "testing", new LocalDate().minusDays(1), Library),
        DeprecatedDependency(d, VersionRange("[2.0.0,2.2.0]"), "testing", new LocalDate().minusDays(1), Library)
      )
    )

    dc.isDependencyValid(deps)(d, Version("1.1.0")) shouldBe MandatoryFail(
      DeprecatedDependency(d, VersionRange("[1.0.0,1.2.0]"), "testing", new LocalDate().minusDays(1), Library))
  }

  it should "return warning if excludes are not applicable yet" in {
    val d                   = Dependency("uk.gov.hmrc", "some-service")
    val tomorrow: LocalDate = new LocalDate().plusDays(1)
    val deps                = (List(DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", tomorrow, Library)))
    dc.isDependencyValid(deps)(d, Version("0.1.0")) shouldBe MandatoryWarn(
      DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", tomorrow, Library))

  }

  it should "return fail if exclude is applicable from today" in {
    val d                = Dependency("uk.gov.hmrc", "some-service")
    val today: LocalDate = new LocalDate()
    val deps             = (List(DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", today, Library)))
    dc.isDependencyValid(deps)(d, Version("0.1.0")) shouldBe MandatoryFail(
      DeprecatedDependency(d, VersionRange("(,1.0.0]"), "testing", today, Library))

  }

  it should "return failed result if the version is in both restricted warn and fail ranges" in {

    val d                        = Dependency("uk.gov.hmrc", "some-service")
    val validTomorrow: LocalDate = new LocalDate().plusDays(1)
    val validToday: LocalDate    = new LocalDate().minusDays(1)
    val deps = (
      List(
        DeprecatedDependency(d, VersionRange("[1.0.0,1.2.0]"), "testing1", validTomorrow, Library),
        DeprecatedDependency(d, VersionRange("[1.0.0,2.2.0]"), "testing2", validToday, Library)
      )
    )

    dc.isDependencyValid(deps)(d, Version("1.1.0")) shouldBe MandatoryFail(
      DeprecatedDependency(d, VersionRange("[1.0.0,2.2.0]"), "testing2", validToday, Library))
  }

  it should "filter non-relevant deprecated dependencies and return success" in {

    val d     = Dependency("uk.gov.hmrc", "some-service")
    val other = Dependency("uk.gov.hmrc", "some-other-service")
    val deps = (
      List(
        DeprecatedDependency(other, VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1), Library),
        DeprecatedDependency(other, VersionRange("[1.0.0,1.2.0]"), "testing", new LocalDate().minusDays(1), Library),
        DeprecatedDependency(other, VersionRange("[2.0.0,2.2.0]"), "testing", new LocalDate().minusDays(1), Library)
      )
    )

    dc.isDependencyValid(deps)(d, Version("1.1.0")) shouldBe OK

  }
  it should "filter non-relevant deprecated dependencies and return fail when deprecated" in {

    val d     = Dependency("uk.gov.hmrc", "some-service")
    val other = Dependency("uk.gov.hmrc", "some-other-service")
    val deps = (
      List(
        DeprecatedDependency(other, VersionRange("(,3.0.0]"), "testing", new LocalDate().minusDays(1), Library),
        DeprecatedDependency(d, VersionRange("[2.0.0,2.2.0]"), "testing2", new LocalDate().minusDays(1), Library)
      )
    )

    dc.isDependencyValid(deps)(d, Version("2.1.0")) shouldBe MandatoryFail(
      DeprecatedDependency(d, VersionRange("[2.0.0,2.2.0]"), "testing2", new LocalDate().minusDays(1), Library))
  }

  it should "work when there is no deprecated dependencies" in {
    dc.isDependencyValid(Seq.empty)(Dependency("org", "me"), Version("1.2.3")) shouldBe OK
  }

  it should "return failed result if the version has snapshot and [*-SNAPSHOT] range is set" in {
    val d = Dependency("uk.gov.hmrc", "some-service")
    val deps =
      (List(DeprecatedDependency(d, VersionRange("[*-SNAPSHOT]"), "testing", new LocalDate().minusDays(1), Library)))
    dc.isDependencyValid(deps)(d, Version("0.1.0-SNAPSHOT")) shouldBe MandatoryFail(
      DeprecatedDependency(d, VersionRange("[*-SNAPSHOT]"), "testing", new LocalDate().minusDays(1), Library))
  }

  it should "return ok result if the version has no snapshot and [*-SNAPSHOT] range is set" in {
    val d = Dependency("uk.gov.hmrc", "some-service")
    val deps =
      (List(DeprecatedDependency(d, VersionRange("[*-SNAPSHOT]"), "testing", new LocalDate().minusDays(1), Library)))
    dc.isDependencyValid(deps)(d, Version("0.1.0")) shouldBe OK
  }

  it should "support '*' wildcard in organisation and name" in {
    val deps = (List(
      DeprecatedDependency(
        Dependency("*", "*"),
        VersionRange("[*-SNAPSHOT]"),
        "testing",
        new LocalDate().minusDays(1),
        Library)))
    dc.isDependencyValid(deps)(Dependency("uk.gov.hmrc", "some-service"), Version("0.1.0-SNAPSHOT")) shouldBe MandatoryFail(
      DeprecatedDependency(
        Dependency("*", "*"),
        VersionRange("[*-SNAPSHOT]"),
        "testing",
        new LocalDate().minusDays(1),
        Library))
  }

  it should "support '*' wildcard in name only" in {
    val deps = (List(
      DeprecatedDependency(
        Dependency("uk.gov.hmrc", "*"),
        VersionRange("[*-SNAPSHOT]"),
        "testing",
        new LocalDate().minusDays(1),
        Library)))
    dc.isDependencyValid(deps)(Dependency("uk.gov.hmrc", "some-service"), Version("0.1.0-SNAPSHOT")) shouldBe MandatoryFail(
      DeprecatedDependency(
        Dependency("uk.gov.hmrc", "*"),
        VersionRange("[*-SNAPSHOT]"),
        "testing",
        new LocalDate().minusDays(1),
        Library))
    dc.isDependencyValid(deps)(Dependency("org.scalatest", "some-service"), Version("0.1.0-SNAPSHOT")) shouldBe OK
  }
}
