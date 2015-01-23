package uk.gov.hmrc

import java.net.URL

import sbt.StringUtilities

import scala.io.Source
import scala.util.Try
import scala.xml.NodeSeq

object Core {

  object Version{

    def apply(st:String):Version={
      Version(st.split('.').toSeq)
    }

    def comparator(v1:Version, v2:Version):Boolean = {
      v1.parts.zip(v2.parts).foldLeft(true){ (result, p) =>
        if(result)
          p._1.compare(p._2) == 0
        else
          result
      }
    }

    def isEarlyRelease(v:Version):Boolean={
      Try { Integer.parseInt(v.parts.last) }.isFailure
    }
  }

  def versionsFromNexus(xml:NodeSeq):Seq[Version] ={
    val nodes = xml \ "data" \ "artifact" \ "version"
    nodes.map(n => Version(n.text))
  }

  def getMandatoryVersions(url:URL):Map[OrganizationName, String]={
    Source.fromURL(url)
      .getLines().toSeq
      .filterNot { line => line.startsWith("#") }
      .map { line => removeWhiteSpace(line).split(',') match {
        case Array(org, name, version) => OrganizationName(org, name) -> version
      }}
      .toMap
  }

  def removeWhiteSpace(st:String) = st.replace("\\s+","")

  case class OrganizationName(module:String, revision:String)

  case class Version(parts:Seq[String]){
    override def toString = parts.mkString(".")
  }

}
