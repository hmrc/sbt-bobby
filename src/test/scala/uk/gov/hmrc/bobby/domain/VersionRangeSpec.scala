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

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class VersionRangeSpec extends AnyFlatSpec with Matchers {

  "A VersionRange" should "read (,1.0] as 'x <= 1.0'" in {

    val r = VersionRange("(,1.0.0]")

    r.lowerBound          shouldBe None
    r.lowerBoundInclusive shouldBe false
    r.upperBound          shouldBe Some(Version("1.0.0"))
    r.upperBoundInclusive shouldBe true

  }

  it should "read [1.0.0] as fixed version '1.0.0'" in {

    val r = VersionRange("[1.0.0]")

    r.lowerBound          shouldBe Some(Version("1.0.0"))
    r.lowerBoundInclusive shouldBe true
    r.upperBound          shouldBe Some(Version("1.0.0"))
    r.upperBoundInclusive shouldBe true

  }

  it should "read [1.2.0,1.3.0] as 1.2.0 <= x <= 1.3.0" in {

    val r = VersionRange("[1.2.0,1.3.0]")

    r.lowerBound          shouldBe Some(Version("1.2.0"))
    r.lowerBoundInclusive shouldBe true
    r.upperBound          shouldBe Some(Version("1.3.0"))
    r.upperBoundInclusive shouldBe true

  }

  it should "read [1.0.0,2.0.0) as 1.0.0 <= x < 2.0.0" in {

    val r = VersionRange("[1.0.0,2.0.0)")

    r.lowerBound          shouldBe Some(Version("1.0.0"))
    r.lowerBoundInclusive shouldBe true
    r.upperBound          shouldBe Some(Version("2.0.0"))
    r.upperBoundInclusive shouldBe false

  }

  it should "read [8.0.0,8.4.1] as 8.0.0 <= x <= 8.4.1" in {

    val r = VersionRange("[8.0.0,8.4.1]")

    r.lowerBound          shouldBe Some(Version("8.0.0"))
    r.lowerBoundInclusive shouldBe true
    r.upperBound          shouldBe Some(Version("8.4.1"))
    r.upperBoundInclusive shouldBe true

  }

  it should "read ranges with spaces" in {

    val r = VersionRange("[8.0.0, 8.4.1]")

    r.lowerBound          shouldBe Some(Version("8.0.0"))
    r.lowerBoundInclusive shouldBe true
    r.upperBound          shouldBe Some(Version("8.4.1"))
    r.upperBoundInclusive shouldBe true

  }

  it should "read [1.5.0,) as x >= 1.5.0" in {

    val r = VersionRange("[1.5.0,)")

    r.lowerBound          shouldBe Some(Version("1.5.0"))
    r.lowerBoundInclusive shouldBe true
    r.upperBound          shouldBe None
    r.upperBoundInclusive shouldBe false

  }

  it should "read the '[*-SNAPSHOT]' range'" in {
    val r = VersionRange("[*-SNAPSHOT]")

    r.lowerBound          shouldBe None
    r.lowerBoundInclusive shouldBe false
    r.upperBound          shouldBe None
    r.upperBoundInclusive shouldBe false
    r.qualifierStartsWith shouldBe Some("SNAPSHOT")
  }

  it should "throw an IllegalArgumentException when incomplete version is provided" in {
    an[IllegalArgumentException] should be thrownBy VersionRange("[1.5,)")
  }

  it should "throw an IllegalArgumentException when brackets are missing" in {
    an[IllegalArgumentException] should be thrownBy VersionRange("1.5,)")
    an[IllegalArgumentException] should be thrownBy VersionRange("[1.5,")
    an[IllegalArgumentException] should be thrownBy VersionRange("1.5")
  }

  it should "throw an IllegalArgumentException when the range is open on both sides" in {
    an[IllegalArgumentException] should be thrownBy VersionRange("(,1.5,)")
  }

  it should "throw an IllegalArgumentException when multiple sets are used" in {
    an[IllegalArgumentException] should be thrownBy VersionRange("(,1.0],[1.2,)")
  }

  it should "include 1.2.5 when the expression is [1.2.0,1.3.0]" in {
    VersionRange("[1.2.0,1.3.0]").includes(Version("1.2.5")) shouldBe true
  }

  it should "include 0.2.0 when the expression is (,1.0.0]" in {
    VersionRange("(,1.0.0]").includes(Version("0.2.0")) shouldBe true
  }

  it should "include 1.2.0 when the expression is [1.0.0,)" in {
    VersionRange("[1.0.0,)").includes(Version("1.2.0")) shouldBe true
  }

  it should "not include the left boundary when the expression is (1.0.0,)" in {
    VersionRange("(1.0.0,)").includes(Version("1.0.0")) shouldBe false
  }

  it should "not include the right boundary when the expression is (,1.0.0)" in {
    VersionRange("(,1.0.0)").includes(Version("1.0.0")) shouldBe false
  }

  it should "include snapshots when the version is 1.0.0-SNAPSHOT" in {
    VersionRange("[*-SNAPSHOT]").includes(Version("1.0.0-SNAPSHOT")) shouldBe true
  }

  it should "not include snapshots when the version is 1.0.0" in {
    VersionRange("[*-SNAPSHOT]").includes(Version("1.0.0")) shouldBe false
  }

  it should "build string that contains inclusive lower-bound and upper-bound" in {
    VersionRange("[1.2.0,1.3.0]").toString shouldBe "[1.2.0,1.3.0]"
  }

  it should "build string that contains wildcard" in {
    VersionRange("[*-SNAPSHOT]").toString shouldBe "[*-SNAPSHOT]"
  }

  it should "build string that contains non-inclusive lower-bound and upper-bound" in {
    VersionRange("(1.2.0,1.3.0)").toString shouldBe "(1.2.0,1.3.0)"
  }

  it should "understand play cross compiled libraries and ignore play suffixes" in {
    VersionRange("[1.0.0, 1.0.0]").includes(Version("1.0.0-play-25")) shouldBe true
    VersionRange("[1.0.0, 1.0.0]").includes(Version("1.0.0-play-26")) shouldBe true
  }

  it should "throw exception when qualifier is not defined" in {
    intercept[IllegalArgumentException] {
      VersionRange("[*-]")
    }

    intercept[IllegalArgumentException] {
      VersionRange("[*]")
    }
  }
}
