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

case class BobbyRule(
  dependency: Dependency,
  range: VersionRange,
  reason: String,
  effectiveDate: LocalDate)

object BobbyRule {

  // In some cases multiple bobby rules are found which may be activated, so we need to prioritise which one is the
  // chosen one.
  // e.g. you might create a rule on reactive mongo 0.17.0 because of a memory leak. Later on you might create a new
  // rule to outlaw all version of reactive mongo.
  // If we now try to use reactivemongo 0.17.0 in our build, both bobby rules apply. In this case, we want to take the
  // second one as it is more encompassing.
  //
  // The priority we apply is below, in decreasing order of precedence:
  // 1. First take the upper bound of the rule version. The one that is the *highest* takes precedence
  //    a. No upper bound (None) first
  //    b. Highest upper bound if both defined
  // 2. Inclusive upper bound over exclusive (when version numbers matching)
  // 3. Most recent rule first
  // 4. Undefined (very much an edge case, pick any)
  // This ordering matches the ones used by the Catalogue
  implicit val ordering = new Ordering[BobbyRule] {
    // ordering by rule which is most strict first
    def compare(x: BobbyRule, y: BobbyRule): Int = {
      implicit val vo : Ordering[Version]   = new Ordering[Version] {
        override def compare(x: Version, y: Version): Int = x.compareTo(y)
      }.reverse
      implicit val bo : Ordering[Boolean]   = Ordering.Boolean.reverse
      implicit val ldo: Ordering[LocalDate] = new Ordering[LocalDate] {
        def compare(x: LocalDate, y: LocalDate): Int = x.compareTo(y)
      }.reverse

      val first = (x.range.upperBound, x.range.upperBoundInclusive, x.effectiveDate)
      val second = (y.range.upperBound, y.range.upperBoundInclusive, y.effectiveDate)

      import scala.math.Ordering.Tuple3
      Ordering[(Option[Version], Boolean, LocalDate)].compare(first, second)
    }
  }
}
