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

case class VersionRange(lowerBound: Option[Version], lowerBoundInclusive: Boolean, upperBound: Option[Version], upperBoundInclusive: Boolean, qualifierStartsWith: Option[String] = None) {

  def includes(version: Version): Boolean = {

    if (qualifierStartsWith.isDefined) {
      version.buildOrQualifier match {
        case Some(Right(q)) if q.toUpperCase.startsWith(qualifierStartsWith.get.toUpperCase) => true
        case _ => false
      }
    } else {

      val lbRange = lowerBound.fold(true)(lb => version.isAfter(lb) || (lowerBoundInclusive && lb.equals(version)))
      val ubRange = upperBound.fold(true)(ub => version.isBefore(ub) || (upperBoundInclusive && ub.equals(version)))
      lbRange && ubRange
    }

  }
}

  /**
   *
   * Supporting the following expressions:
   * Range               | Meaning
   * (,1.0.0]            | x <= 1.0.0
   * [1.0.0]             | Hard requirement on 1.0.0
   * [1.2.0,1.3.0]       | 1.2.0 <= x <= 1.3.0
   * [1.0.0,2.0.0)       | 1.0.0 <= x < 2.0.0
   * [1.5.0,)            | x >= 1.5.0
   * [*-SNAPSHOT]        | Any version with qualifier 'SNAPSHOT'
   *
   * All versions must have all 3 numbers, 1.0 is not supported for example
   *
   * @throws IllegalArgumentException when an illegal format is used
   */
  object VersionRange {

    implicit def toVersion(v: String): Version = Version(v)

    val ValidFixedVersion = """^\[(\d+\.\d+.\d+)\]""".r
    val ValidVersionRangeLeftOpen = """^\(,?(\d+\.\d+.\d+)[\]\)]""".r
    val ValidVersionRangeRightOpen = """^[\[\(](\d+\.\d+.\d+),[\]\)]""".r
    val ValidVersionRangeBetween = """^[\[\(](\d+\.\d+.\d+),(\d+\.\d+.\d+)[\]\)]""".r
    val Qualifier = """^\[[-\*]+(.*)\]""".r

    def apply(range: String): VersionRange = {
      range.replaceAll(" ", "") match {
        case ValidFixedVersion(v) => VersionRange(Some(v), true, Some(v), true)
        case ValidVersionRangeLeftOpen(v) => VersionRange(None, false, Some(v), range.endsWith("]"))
        case ValidVersionRangeRightOpen(v) => VersionRange(Some(v), range.startsWith("["), None, false)
        case ValidVersionRangeBetween(v1, v2) => VersionRange(Some(v1), range.startsWith("["), Some(v2), range.endsWith("]"))
        case Qualifier(q) => VersionRange(None, false, None, false, Some(q))
        case _ => throw new IllegalArgumentException(s"'$range' is not a valid range expression")
      }
    }
  }
