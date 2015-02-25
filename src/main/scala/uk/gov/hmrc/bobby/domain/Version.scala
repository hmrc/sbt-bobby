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
    case 0  => p1.compare(p2)
    case _ => result
  }}

  override def equals(obj: scala.Any): Boolean = obj match {
    case v: Version => compareTo(v) == 0
    case _ => false
  }
}
