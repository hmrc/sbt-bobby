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

package uk.gov.hmrc.bobby

import org.joda.time.LocalDate
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{AppendedClues, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sbt.librarymanagement.ModuleID
import uk.gov.hmrc.bobby.Generators._
import uk.gov.hmrc.bobby.domain._

trait BaseSpec
  extends AnyWordSpecLike
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with OptionValues
    with AppendedClues

class BobbyValidatorSpec extends BaseSpec {

  "BobbyValidator.calc" should {

    "give BobbyViolation if time is later than rule" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val d = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", LocalDate.now(), t)
        val m = ModuleID(d.dependency.organisation, d.dependency.name, "0.1.0")
        BobbyValidator.calc(List(d), m) shouldBe BobbyViolation(d)
      }
    }

    "give BobbyViolation if time is equal to rule" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val now = LocalDate.now()
        val d = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, t)
        val m = ModuleID(d.dependency.organisation, d.dependency.name, "0.1.0")
        BobbyValidator.calc(List(d), m, now) shouldBe BobbyViolation(d)
      }
    }

    "give BobbyWarning if time is before rule" in {
      forAll(depedendencyGen, dependencyTypeGen) { case(dep, t) =>
        val now = LocalDate.now()
        val d = BobbyRule(dep, VersionRange("[0.1.0]"), "some reason", now, t)
        val m = ModuleID(d.dependency.organisation, d.dependency.name, "0.1.0")
        BobbyValidator.calc(List(d), m, now.minusDays(1)) shouldBe BobbyWarning(d)
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
