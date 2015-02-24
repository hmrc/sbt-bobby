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
package uk.gov.hmrc.bobby

import java.net.URL

import sbt.{ModuleID, StringUtilities}

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

  def getMandatoryVersions(fileContents:String):Map[OrganizationName, String]={
    fileContents.split('\n')
      .filterNot { line => line.startsWith("#") }
      .map { line => removeWhiteSpace(line).split(',') match {
        case Array(org, name, version) => OrganizationName(org, name) -> version
      }}
      .toMap
  }

  def removeWhiteSpace(st:String) = st.replace("\\s+","")

  case class OrganizationName(module:String, revision:String)
  object OrganizationName{
    def apply(module:ModuleID):OrganizationName = OrganizationName(module.organization, module.name)
  }

  case class Version(parts:Seq[String]){
    override def toString = parts.mkString(".")
  }

}
