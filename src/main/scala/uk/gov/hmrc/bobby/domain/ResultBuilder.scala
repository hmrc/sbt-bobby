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

package uk.gov.hmrc.bobby.domain

import sbt.{ConsoleLogger, ModuleID}

import scala.util.{Failure, Success, Try}

object ResultBuilder {

  val logger = ConsoleLogger()

  def calculate(
    dependencyMap: Map[ModuleID, Seq[ModuleID]],
    projectLibraries: Seq[ModuleID],
    projectPlugins: Seq[ModuleID],
    latestRepoLibraries: Option[Map[ModuleID, Try[Version]]],
    deprecatedDependencies: DeprecatedDependencies): List[Message] = {

    val pluginMessages = checkMandatoryMessages(deprecatedDependencies.plugins, projectPlugins, dependencyMap)
    val libMessages    = libraryMessages(dependencyMap, projectLibraries, latestRepoLibraries, deprecatedDependencies.libs)

    (pluginMessages ::: libMessages).sortBy(_.moduleName)

  }

  private def libraryMessages(
    dependencyMap: Map[ModuleID, Seq[ModuleID]],
    projectLibraries: Seq[ModuleID],
    latestRepoLibraries: Option[Map[ModuleID, Try[Version]]],
    deprecatedLibraries: List[DeprecatedDependency]): List[Message] = {
    val mandatoryLibMessages = checkMandatoryMessages(deprecatedLibraries, projectLibraries, dependencyMap)
    val repositoryMessages = latestRepoLibraries
      .map { ld =>
        calculateRepositoryResults(ld, dependencyMap)
      }
      .getOrElse(List.empty[Message])

    val repoOnlyMessages = repositoryMessages.filterNot { m =>
      mandatoryLibMessages.map(_.moduleName).contains(m.moduleName)
    }

    val (mandatoryAndRepoMessages, mandatoryOnlyMessages) = mandatoryLibMessages.partition { m =>
      repositoryMessages.map(_.moduleName).contains(m.moduleName)
    }

    val updatedMandatoryAndRepoMessages = mandatoryAndRepoMessages
      .map { m =>
        m -> repositoryMessages.find(_.moduleName == m.moduleName)
      }
      .collect { case (mm, Some(rm)) => mm.copy(latestRevisionT = rm.latestRevisionT) }

    repoOnlyMessages ++ mandatoryOnlyMessages ++ updatedMandatoryAndRepoMessages
  }

  def checkMandatoryMessages(excludes: Seq[DeprecatedDependency], projectVersions: Seq[ModuleID], dependencyMap: Map[ModuleID, Seq[ModuleID]]): List[Message] = {
    val isValid = DependencyChecker.isDependencyValid(excludes) _

    projectVersions.toList.flatMap { module =>
      isValid(Dependency(module.organization, module.name), Version(module.revision)) match {
        case MandatoryFail(exclusion) =>
          Some(new Message(DependencyUnusable, module, dependencyMap.getOrElse(module, Seq.empty), Failure(new Exception("(check repo)")), Some(exclusion)))

        case MandatoryWarn(exclusion) =>
          Some(new Message(DependencyNearlyUnusable, module, dependencyMap.getOrElse(module, Seq.empty), Failure(new Exception("(check repo)")), Some(exclusion)))

        case _ => None
      }
    }
  }

  def calculateRepositoryResults(latestRevisions: Map[ModuleID, Try[Version]], dependencyMap: Map[ModuleID, Seq[ModuleID]]): List[Message] =
    latestRevisions.toList.flatMap {
      case (module, f @ Failure(ex)) =>
        Some(new Message(UnknownVersion, module, dependencyMap.getOrElse(module, Seq.empty), f, None))

      case (module, Success(latestRevision)) if latestRevision.isAfter(Version(module.revision)) =>
        Some(new Message(NewVersionAvailable, module, dependencyMap.getOrElse(module, Seq.empty), Success(latestRevision), None))

      case a @ _ =>
        None
    }
}
