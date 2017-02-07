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

package uk.gov.hmrc.bobby.domain

import sbt.{ConsoleLogger, ModuleID}
import uk.gov.hmrc.bobby._

import scala.util.{Failure, Success, Try}

object ResultBuilder {

  val logger = ConsoleLogger()

  def calculate(assetsDependencies: Seq[ModuleID], projectLibraries: Seq[ModuleID], projectPlugins: Seq[ModuleID], latestAssetsRevision: Option[Map[ModuleID, Try[Version]]], latestRepoLibraries: Option[Map[ModuleID, Try[Version]]], deprecatedDependencies: DeprecatedDependencies): List[Message] = {

    val assetsMessages = libraryMessages(assetsDependencies, latestAssetsRevision, deprecatedDependencies.assets)
    val pluginMessages = checkMandatoryMessages(deprecatedDependencies.plugins, projectPlugins)
    val libMessages = libraryMessages(projectLibraries, latestRepoLibraries, deprecatedDependencies.libs)

    (assetsMessages ::: pluginMessages ::: libMessages).sortBy(_.moduleName)
  }

  private def libraryMessages(projectLibraries: Seq[ModuleID], latestRepoLibraries: Option[Map[ModuleID, Try[Version]]], deprecatedLibraries: List[DeprecatedDependency]): List[Message] = {
    val mandatoryLibMessages = checkMandatoryMessages(deprecatedLibraries, projectLibraries)
    val repositoryMessages = latestRepoLibraries.map { ld =>
      calculateRepositoryResults(ld)
    }.getOrElse(List.empty[Message])

    val repoOnlyMessages = repositoryMessages.filterNot { m =>
      mandatoryLibMessages.map(_.moduleName).contains(m.moduleName)
    }


    val (mandatoryAndRepoMessages, mandatoryOnlyMessages) = mandatoryLibMessages.partition { m =>
      repositoryMessages.map(_.moduleName).contains(m.moduleName)
    }


    val updatedMandatoryAndRepoMessages = mandatoryAndRepoMessages
      .map { m => m -> repositoryMessages.find(_.moduleName == m.moduleName) }
      .collect { case (mm, Some(rm)) => mm.copy(latestRevisionT = rm.latestRevisionT) }

    repoOnlyMessages ++ mandatoryOnlyMessages ++ updatedMandatoryAndRepoMessages
  }

  def checkMandatoryMessages(excludes: Seq[DeprecatedDependency], projectVersions: Seq[ModuleID]): List[Message] = {
    val isValid = DependencyChecker.isDependencyValid(excludes) _

    projectVersions.toList.flatMap { module =>
      isValid(Dependency(module.organization, module.name), Version(module.revision)) match {
        case MandatoryFail(exclusion) =>
          Some(new Message(DependencyUnusable, module, Failure(new Exception("(check repo)")), Some(exclusion)))

        case MandatoryWarn(exclusion) =>
          Some(new Message(DependencyNearlyUnusable, module, Failure(new Exception("(check repo)")), Some(exclusion)))

        case _ => None
      }
    }
  }


  def calculateRepositoryResults(latestRevisions: Map[ModuleID, Try[Version]]): List[Message] = {
    latestRevisions.toList.flatMap {
      case (module, f@Failure(ex)) =>
        Some(new Message(UnknownVersion, module, f, None))

      case (module, Success(latestRevision)) if latestRevision.isAfter(Version(module.revision)) =>
        Some(new Message(NewVersionAvailable, module, Success(latestRevision), None))

      case a@_ =>
        None
    }
  }
}
