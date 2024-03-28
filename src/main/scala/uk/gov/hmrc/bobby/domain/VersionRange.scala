/*
 * Copyright 2024 HM Revenue & Customs
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

import scala.util.matching.Regex

case class VersionRange(
  lowerBound         : Option[Version],
  lowerBoundInclusive: Boolean,
  upperBound         : Option[Version],
  upperBoundInclusive: Boolean,
  qualifierStartsWith: Option[String] = None
) {
  def includes(version: Version): Boolean =
    if (qualifierStartsWith.isDefined)
      version.buildOrQualifier match {
        case Some(Right(q)) if q.toUpperCase.startsWith(qualifierStartsWith.get.toUpperCase) => true
        case _                                                                               => false
      }
    else {
      val lbRange = lowerBound.fold(true)(lb => version.isAfter(lb)  || (lowerBoundInclusive && lb.equals(version)))
      val ubRange = upperBound.fold(true)(ub => version.isBefore(ub) || (upperBoundInclusive && ub.equals(version)))
      lbRange && ubRange
    }

  override def toString: String =
    qualifierStartsWith match {
      case Some(q) => s"[*-$q]"
      case _       => Seq(
                        if (lowerBoundInclusive) "[" else "(",
                        lowerBound.map(_.toString).getOrElse(""),
                        ",",
                        upperBound.map(_.toString).getOrElse(""),
                         if (upperBoundInclusive) "]" else ")"
                      ).mkString
    }
}

/**
  *
  * Supporting the following expressions:
  * Range               | Meaning
  * "(,1.0.0]"          | x <= 1.0.0
  * "[1.0.0]"           | Hard requirement on 1.0.0
  * "[1.2.0,1.3.0]"     | 1.2.0 <= x <= 1.3.0
  * "[1.0.0,2.0.0)"     | 1.0.0 <= x < 2.0.0
  * "[1.5.0,)"          | x >= 1.5.0
  * "[*-SNAPSHOT]"      | Any version with qualifier 'SNAPSHOT'
  * "*"                 | Any version
  *
  * All versions must have all 3 numbers, 1.0 is not supported for example
  *
  * throws IllegalArgumentException when an illegal format is used
  */
object VersionRange {
  val ValidFixedVersion         : Regex = """^\[(\d+\.\d+.\d+)\]""".r
  val ValidVersionRangeLeftOpen : Regex = """^\(,?(\d+\.\d+.\d+)[\]\)]""".r
  val ValidVersionRangeRightOpen: Regex = """^[\[\(](\d+\.\d+.\d+),[\]\)]""".r
  val ValidVersionRangeBetween  : Regex = """^[\[\(](\d+\.\d+.\d+),(\d+\.\d+.\d+)[\]\)]""".r
  val Qualifier                 : Regex = """^\[[-\*]+(.*)\]""".r

  def apply(range: String): VersionRange =
    (if (range.trim == "*") "[0.0.0,)" else range.replaceAll(" ", "")) match {
      case ValidFixedVersion(v)             => VersionRange(Some(Version(v)), true, Some(Version(v)), true)
      case ValidVersionRangeLeftOpen(v)     => VersionRange(None, false, Some(Version(v)), range.endsWith("]"))
      case ValidVersionRangeRightOpen(v)    => VersionRange(Some(Version(v)), range.startsWith("["), None, false)
      case ValidVersionRangeBetween(v1, v2) => VersionRange(Some(Version(v1)), range.startsWith("["), Some(Version(v2)), range.endsWith("]"))
      case Qualifier(q) if q.length() > 1   => VersionRange(None, false, None, false, Some(q))
      case _                                => throw new IllegalArgumentException(s"'$range' is not a valid range expression")
    }
}
