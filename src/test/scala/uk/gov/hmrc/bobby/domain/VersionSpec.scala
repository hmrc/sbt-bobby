/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatest.{FlatSpec, Matchers}

class VersionSpec extends FlatSpec with Matchers {

  implicit def toLeft(i: Int) = Left(i)

  implicit def toRight(s: String) = Right(s)


  "Version parsing" should "work for '1'" in {
    Version("1") shouldBe Version(1, 0, 0, None)
  }
  it should "work for '1.2'" in {
    Version("1.2") shouldBe Version(1, 2, 0, None)
  }
  it should "work for '1.2.3'" in {
    Version("1.2.3") shouldBe Version(1, 2, 3, None)
  }
  it should "work for '0.1.0'" in {
    Version("0.1.0") shouldBe Version(0, 1, 0, None)
  }
  it should "work for '1.2.3-4'" in {
    Version("1.2.3-4") shouldBe Version(1, 2, 3, Some(Left(4)))
  }
  it should "work for '1.2.3-alpha-1'" in {
    Version("1.2.3-alpha-1") shouldBe Version(1, 2, 3, Some("alpha-1"))
  }
  it should "work for '1.2-alpha-1-20050205.060708-1'" in {
    Version("1.2-alpha-1-20050205.060708-1") shouldBe Version(1, 2, 0, Some("alpha-1-20050205.060708-1"))
  }
  it should "work for '999-SNAPSHOT'" in {
    Version("999-SNAPSHOT") shouldBe Version(999, 0, 0, Some("SNAPSHOT"))
  }
  it should "work for '2.0-1'" in {
    Version("2.0-1") shouldBe Version(2, 0, 0, Some(Left(1)))
  }
  it should "work for '1.2.3-RC4'" in {
    Version("1.2.3-RC-4") shouldBe Version(1, 2, 3, Some("RC-4"))
  }

  "Unsupported version schemas" should "transformed into qualifiers" in {
    Version("1.0.1b") shouldBe Version(0, 0, 0, Some("1.0.1b"))
    Version("1.0M2") shouldBe Version(0, 0, 0, Some("1.0M2"))
    Version("1.0RC2") shouldBe Version(0, 0, 0, Some("1.0RC2"))
    Version("1.7.3.0") shouldBe Version(0, 0, 0, Some("1.7.3.0"))
    Version("1.7.3.0-1") shouldBe Version(0, 0, 0, Some("1.7.3.0-1"))
    Version("PATCH-1193602") shouldBe Version(0, 0, 0, Some("PATCH-1193602"))
    Version("5.0.0alpha-2006020117") shouldBe Version(0, 0, 0, Some("5.0.0alpha-2006020117"))
    Version("1.0.0.-SNAPSHOT") shouldBe Version(0, 0, 0, Some("1.0.0.-SNAPSHOT"))
    Version("1..0-SNAPSHOT") shouldBe Version(0, 0, 0, Some("1..0-SNAPSHOT"))
    Version("1.0.-SNAPSHOT") shouldBe Version(0, 0, 0, Some("1.0.-SNAPSHOT"))
    Version(".1.0-SNAPSHOT") shouldBe Version(0, 0, 0, Some(".1.0-SNAPSHOT"))
  }

  "1.0.0" should "be greater than 0.0.1" in {
    Version("1.0.0").isAfter(Version("0.0.1")) shouldBe true
  }

  "2.11.2" should "be greater than 2.9.3" in {
    Version("2.11.2").isAfter(Version("2.9.3")) shouldBe true
  }

  "1.0.0" should "be smaller than 1.0.1" in {
    Version("1.0.0").isBefore(Version("1.0.1")) shouldBe true
  }

  "1.0.1" should "be equal to 1.0.1" in {
    Version("1.0.1").equals(Version("1.0.1")) shouldBe true
  }

  "1.0.1" should "not be equal to 1.0.1-SNAPSHOT" in {
    Version("1.0.1").equals(Version("1.0.1-SNAPSHOT")) shouldBe false
  }

  "1.0.1" should "be after to 1.0.1-SNAPSHOT" in {
    Version("1.0.1").isAfter(Version("1.0.1-SNAPSHOT")) shouldBe true
  }

  "Version comparison" should "work for '1.0-alpha-1' < 1.0" in {
    Version("1.0-alpha-1").isBefore(Version("1.0")) shouldBe true
  }


  it should "work for '1.0-alpha-1' < 1.0-alpha-2" in {
    Version("1.0-alpha-1").isBefore(Version("1.0-alpha-2")) shouldBe true
  }

  it should "work for '1.0-alpha-1' < 1.0-beta-2" in {
    Version("1.0-alpha-1").isBefore(Version("1.0-beta-1")) shouldBe true
  }

  it should "work for '1.0-alpha-2' < 1.0-alpha-15" in {
    Version("1.0-alpha-2").isBefore(Version("1.0-beta-15")) shouldBe true
  }

  it should "work for '1.0-alpha-1' < 1.0-beta-1" in {
    Version("1.0-alpha-1").isBefore(Version("1.0-beta-1")) shouldBe true
  }

  it should "work for '1.0-SNAPSHOT' < 1.0-beta-1" in {
    Version("1.0-SNAPSHOT").isBefore(Version("1.0-beta-1")) shouldBe true
  }

  it should "work for '1.0-SNAPSHOT' < 1.0" in {
    Version("1.0-SNAPSHOT").isBefore(Version("1.0")) shouldBe true
  }

  it should "work for '1.0-alpha-1-SNAPSHOT' < 1.0-alpha-2" in {
    Version("1.0-alpha-1-SNAPSHOT").isBefore(Version("1.0-alpha-2")) shouldBe true
  }

  it should "work for '1.0' < 1.0-1" in {
    Version("1.0").isBefore(Version("1.0-1")) shouldBe true
  }
  it should "work for 1.0-1 < 1.0-2" in {
    Version("1.0-1").isBefore(Version("1.0-2")) shouldBe true
  }
  it should "work for 2.0 < 2.0-1" in {
    Version("2.0").isBefore(Version("2.0-1")) shouldBe true
  }
  it should "work for 2.0.0 < 2.0-1" in {
    Version("2.0.0").isBefore(Version("2.0-1")) shouldBe true
  }
  it should "work for 2.0-1 < 2.0.1" in {
    Version("2.0-1").isBefore(Version("2.0.1")) shouldBe true
  }
  it should "work for 2.0.1-klm < 2.0.1-lmn" in {
    Version("2.0.1-klm").isBefore(Version("2.0.1-lmn")) shouldBe true
  }
  it should "work for 2.0.1 < 2.0.1-123" in {
    Version("2.0.1").isBefore(Version("2.0.1-123")) shouldBe true
  }
  it should "work for 2.0.1-xyz < 2.0.1-123" in {
    Version("2.0.1-xyz").isBefore(Version("2.0.1-123")) shouldBe true
  }
  it should "work for '1.2.3-10000000000' < 1.2.3-10000000001" in {
    Version("1.2.3-10000000000").isBefore(Version("1.2.3-10000000001")) shouldBe true
  }
  it should "work for '1.2.3-1' < 1.2.3-10000000001" in {
    Version("1.2.3-1").isBefore(Version("1.2.3-10000000001")) shouldBe true
  }
  it should "work for '2.3.0-v200706262000' < 2.3.0-v200706262130" in {
    Version("2.3.0-v200706262000").isBefore(Version("2.3.0-v200706262130")) shouldBe true
  }
  it should "work for '2.0.0.v200706041905-7C78EK9E_EkMNfNOd2d8qq' < 2.0.0.v200706041906-7C78EK9E_EkMNfNOd2d8qq" in {
    Version("2.0.0.v200706041905-7C78EK9E_EkMNfNOd2d8qq").isBefore(Version("2.0.0.v200706041906-7C78EK9E_EkMNfNOd2d8qq")) shouldBe true
  }

  "Version" should "recognise an early release" in {
    Version.isSnapshot(Version(2,2,3, Some(Right("SNAPSHOT")))) shouldBe true
    Version.isSnapshot(Version(2,2,2)) shouldBe false
  }

  it should "recognise '*-SNAP1' as a snapshot" in {
    Version.isSnapshot(Version(2,2,3, Some(Right("M1")))) shouldBe true
  }

  it should "recognise '*-M1' as a snapshot" in {
    Version.isSnapshot(Version(2,2,3, Some(Right("M1")))) shouldBe true
  }

  it should "recognise '*-FINAL' as a release" in {
    Version.isSnapshot(Version(2,2,3, Some(Right("FINAL")))) shouldBe false
  }

  it should "recognise '2.3.0_0.1.8' as a release" in {
    Version.isSnapshot(Version("2.3.0_0.1.8")) shouldBe false
  }

  it should "correctly print qualifiers" in {
    Version("2.0.1-klm").toString shouldBe "2.0.1-klm"
  }
}
