/*
 * Copyright 2016 HM Revenue & Customs
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

import sbt._
import java.net.URL
import scala.util.Try
import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.Repo
import uk.gov.hmrc.bobby.conf.{ApplicationConfig, Configuration}
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{Output, JsonOutingFileWriter, Tabulator, TextOutingFileWriter}
import uk.gov.hmrc.bobby.repos.Repositories

class BobbyValidationFailedException(message: String) extends RuntimeException(message)

object Bobby {

  private val logger = ConsoleLogger()
  private val currentVersion = getClass.getPackage.getImplementationVersion

  val ignoredOrgs = Set(
    "com.typesafe.play",
    "com.kenshoo",
    "com.codahale.metrics",
    "org.scala-lang"
  )

  val assetsDependencies = {
    val appConf = ConfigFactory.parseFile(new File("conf/application.conf"))
    val applicationConfig = new ApplicationConfig(appConf)
    val assetsFrontendVersion = applicationConfig.assetsFrontendVersion

    if (assetsFrontendVersion.isDefined) {
      Seq(ModuleID("", "assets-frontend", assetsFrontendVersion.get))
    } else Seq.empty[ModuleID]
  }

  def validateDependencies(libraries: Seq[ModuleID],
                           plugins: Seq[ModuleID],
                           scalaVersion: String,
                           reposValue: Seq[Repo],
                           checkForLatest: Boolean,
                           deprecatedDependenciesUrl: Option[URL] = None,
                           jsonOutputFileOverride: Option[String] = None,
                           isSbtProject: Boolean = false) = {

    logger.info(s"[bobby] Bobby version $currentVersion")

    val config = new Configuration(deprecatedDependenciesUrl, jsonOutputFileOverride)

    val filteredLibraries = filterDependencies(libraries, ignoredOrgs)

    val latestLibraryRevisionsO = if (checkForLatest) {
      Some(findLatestVersions(Some(scalaVersion), reposValue, filteredLibraries))
    } else None

    val latestAssetsRevision = if (checkForLatest) {
      Some(findLatestVersions(None, reposValue, assetsDependencies))
    } else None

    val messages = ResultBuilder.calculate(assetsDependencies, filteredLibraries, plugins, latestAssetsRevision, latestLibraryRevisionsO, config.loadDeprecatedDependencies)

    Output.outputMessages(messages, config.jsonOutputFile, config.textOutputFile)

    if (messages.exists(_.isError))
      throw new BobbyValidationFailedException("See previous bobby output for more information")
  }

  def findLatestVersions(scalaVersion: Option[String], repositoriesToCheck: Seq[Repo], prepared: Seq[ModuleID]): Map[ModuleID, Try[Version]] = {
    val repoSearch = Repositories.buildAggregateRepositories(repositoriesToCheck)
    getLatestRepoRevisions(scalaVersion, prepared, repoSearch)
  }

  private[bobby] def filterDependencies(dependencies: Seq[ModuleID], ignoreList: Set[String]): Seq[ModuleID] = {
    compactDependencies(dependencies)
      .filterNot(m => ignoreList.contains(m.organization))
  }

  private[bobby] def getLatestRepoRevisions(
                                             scalaVersion: Option[String],
                                             compacted: Seq[ModuleID],
                                             repoSearch: RepoSearch
                                             ): Map[ModuleID, Try[Version]] = {
    compacted.par.map { module =>
      module -> repoSearch.findLatestRevision(module, scalaVersion)
    }.seq.toMap
  }


  private[bobby] def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    def orgAndName(d: ModuleID) = s"${d.organization}.${d.name}"

    dependencies
      .groupBy(orgAndName)
      .map(_._2.head)
      .toSeq
  }

}
