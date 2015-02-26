/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.bobby.domain

import scala.util.Try

object Version{

  def apply(st:String):Version = Version(st.split('.').toSeq)

  def comparator(v1:Version, v2:Version):Boolean = v1.isAfter(v2)

  def isEarlyRelease(v:Version):Boolean={
    Try { Integer.parseInt(v.parts.last) }.isFailure
  }
}

case class Version(parts:Seq[String]) extends Comparable [Version] {

  def isBefore(version: Version): Boolean =  this.compareTo(version) < 0

  def isAfter(version: Version) = this.compareTo(version) > 0

  override def toString = parts.mkString(".")

  override def compareTo(version: Version): Int =
    parts.zip(version.parts).foldLeft(0){ case(result, (p1, p2)) => result match {
    case 0 if isAllDigits(p1) && isAllDigits(p2)  => p1.toInt.compare(p2.toInt)
    case 0 => p1.compare(p2)
    case _ => result
  }}

  private def isAllDigits(x: String) = x forall Character.isDigit

  override def equals(obj: scala.Any): Boolean = obj match {
    case v: Version => compareTo(v) == 0
    case _ => false
  }
}
