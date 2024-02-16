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

import scala.util.Try

object Version {

  private def isAllDigits(x: String) = x forall Character.isDigit

  def apply(versionString: String): Version = {
    val split = versionString.split("[-_]", 2)
    val versionNumberParts = toVer(split.headOption.getOrElse("0.0.0"))
    val boq   = toBuildOrQualifier(ignorePlaySuffixForCrossCompiledLibs(split.lift(1)))

    versionNumberParts match {
      case (0, 0, 0) => Version(0, 0, 0, Some(Right(versionString)))
      case (major, minor, patch) => Version(major, minor, patch, boq)
    }
  }

  def toVer(v: String): (Int, Int, Int) = {
    val version1Regex = """(\d+)""".r
    val version2Regex = """(\d+)\.(\d+)""".r
    val version3Regex = """(\d+)\.(\d+)\.(\d+)""".r

    def toInt(str: String) = Try(str.toInt).getOrElse(0)

    v match {
      case version1Regex(major) => (toInt(major), 0, 0)
      case version2Regex(major, minor) => (toInt(major), toInt(minor), 0)
      case version3Regex(major, minor, patch) => (toInt(major), toInt(minor), toInt(patch))
      case _ => (0,0,0)
    }
  }

  def toBuildOrQualifier(boqStOpt: Option[String]): Option[Either[Long, String]] =
    boqStOpt.map(boqSt => if (isAllDigits(boqSt)) Left(boqSt.toLong) else Right(boqSt))

  def ignorePlaySuffixForCrossCompiledLibs(maybeQualifier: Option[String]): Option[String] =
    maybeQualifier.flatMap {
      case s if s.contains("play-2") => None
      case s                         => Some(s)
    }

  def comparator(v1: Version, v2: Version): Boolean = v1.isAfter(v2)

  def isSnapshot(v: Version): Boolean = v.buildOrQualifier match {
    case Some(Right("SNAPSHOT"))                            => true
    case Some(Right(s)) if s.toLowerCase.startsWith("snap") => true
    case Some(Right(s)) if s.toLowerCase.startsWith("m")    => true
    case _                                                  => false
  }
}

case class Version(major: Int, minor: Int, revision: Int, buildOrQualifier: Option[Either[Long, String]] = None)
    extends Comparable[Version] {

  def isBefore(version: Version): Boolean = this.compareTo(version) < 0

  def isAfter(version: Version) = this.compareTo(version) > 0

  lazy val boqFormatted: Option[String] = buildOrQualifier.map {
    case Left(num) => num.toString
    case Right(st) => st
  }

  override def toString = s"$major.$minor.$revision${boqFormatted.map(b => "-" + b).getOrElse("")}"

  val parts = List(major, minor, revision)

  override def compareTo(version: Version): Int = {

    val partsComparison = parts.zip(version.parts).foldLeft(0) {
      case (result, (p1, p2)) =>
        result match {
          case 0 => p1.compare(p2)
          case _ => result
        }
    }

    partsComparison match {
      case 0 =>
        buildOrQualifier -> version.buildOrQualifier match {
          case (None, None)                       => 0
          case (None, Some(Left(b)))              => -1
          case (None, Some(Right(q)))             => 1
          case (Some(Left(b)), None)              => 1
          case (Some(Right(q)), None)             => -1
          case (Some(Left(b1)), Some(Right(s2)))  => 1
          case (Some(Right(s1)), Some(Left(b2)))  => -1
          case (Some(Left(b1)), Some(Left(b2)))   => b1.compareTo(b2)
          case (Some(Right(s1)), Some(Right(s2))) => s1.compareTo(s2)
        }
      case _ => partsComparison
    }
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case v: Version => compareTo(v) == 0
    case _          => false
  }
}
