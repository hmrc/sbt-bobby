/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.bobby

import java.net.URL

import sbt._
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.Repo
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.Output
import uk.gov.hmrc.bobby.repos.Repositories

import scala.util.Try

class BobbyValidationFailedException(message: String) extends RuntimeException(message)

object Bobby {

  private val logger         = ConsoleLogger()
  private val currentVersion = getClass.getPackage.getImplementationVersion

  val ignoredOrgs = Set(
    "com.typesafe.play",
    "com.kenshoo",
    "com.codahale.metrics",
    "org.scala-lang"
  )

  def validateDependencies(
    libraries: Seq[ModuleID],
    plugins: Seq[ModuleID],
    scalaVersion: String,
    reposValue: Seq[Repo],
    checkForLatest: Boolean,
    deprecatedDependenciesUrl: Option[URL] = None,
    jsonOutputFileOverride: Option[String] = None,
    isSbtProject: Boolean                  = false) = {

    logger.info(s"[bobby] Bobby version $currentVersion")

    val config = new Configuration(deprecatedDependenciesUrl, jsonOutputFileOverride)

    val filteredLibraries = filterDependencies(libraries, ignoredOrgs)

    val latestLibraryRevisionsO = if (checkForLatest) {
      Some(findLatestVersions(scalaVersion, reposValue, filteredLibraries))
    } else None

    val messages =
      ResultBuilder.calculate(filteredLibraries, plugins, latestLibraryRevisionsO, config.loadDeprecatedDependencies)

    Output.outputMessages(messages, config.jsonOutputFile, config.textOutputFile)

    if (messages.exists(_.isError))
      throw new BobbyValidationFailedException("See previous bobby output for more information")
  }

  def findLatestVersions(
    scalaVersion: String,
    repositoriesToCheck: Seq[Repo],
    prepared: Seq[ModuleID]): Map[ModuleID, Try[Version]] = {
    val repoSearch = Repositories.buildAggregateRepositories(repositoriesToCheck)
    getLatestRepoRevisions(scalaVersion, prepared, repoSearch)
  }

  private[bobby] def filterDependencies(dependencies: Seq[ModuleID], ignoreList: Set[String]): Seq[ModuleID] =
    compactDependencies(dependencies)
      .filterNot(m => ignoreList.contains(m.organization))

  private[bobby] def getLatestRepoRevisions(
    scalaVersion: String,
    compacted: Seq[ModuleID],
    repoSearch: RepoSearch
  ): Map[ModuleID, Try[Version]] =
    compacted.par
      .map { module =>
        module -> repoSearch.findLatestRevision(module, Option(scalaVersion))
      }
      .seq
      .toMap

  private[bobby] def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    def orgAndName(d: ModuleID) = s"${d.organization}.${d.name}"

    dependencies
      .groupBy(orgAndName)
      .map(_._2.head)
      .toSeq
  }

}
