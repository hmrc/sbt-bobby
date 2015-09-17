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

import sbt.ModuleID

import scala.util.{Failure, Success, Try}

trait RepoSearch{

  def repoName:String

  def shortenScalaVersion(scalaVersion: String): String = {
    scalaVersion.split('.') match {
      case Array(major, minor, _*) => major + "." + minor
    }
  }

  def findLatestRevision(versionInformation: ModuleID, scalaVersion: Option[String]): Option[Version] = {
    search(versionInformation, scalaVersion.map{ shortenScalaVersion }) match {
      case Success(s) if s.isDefined => s
      case Success(s) => search(versionInformation, None).toOption.flatten
      case Failure(e) => e.printStackTrace(); None //logger.warn(s"Unable to query nexus: ${e.getClass.getName}: ${e.getMessage}"); None
    }
  }

  def search(versionInformation: ModuleID, scalaVersion: Option[String]):Try[Option[Version]]
}
