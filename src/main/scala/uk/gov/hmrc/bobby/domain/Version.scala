package uk.gov.hmrc.bobby.domain

import scala.util.Try

object Version{

  def apply(st:String):Version={
    Version(st.split('.').toSeq)
  }

  def comparator(v1:Version, v2:Version):Boolean =
    v1.parts.zip(v2.parts).foldLeft(true){ case(result, (p1, p2)) => result match {
      case true  => p1.compare(p2) == 0
      case false => result
    }}

  def isEarlyRelease(v:Version):Boolean={
    Try { Integer.parseInt(v.parts.last) }.isFailure
  }
}

case class Version(parts:Seq[String]){
  override def toString = parts.mkString(".")
}
