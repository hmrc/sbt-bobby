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

import sbt.{ConsoleLogger, ModuleID}
import uk.gov.hmrc.bobby._

import scala.util.{Failure, Success, Try}

object ResultBuilder {

  val logger = ConsoleLogger()

  def calculate(projectDependencies:Seq[ModuleID],
                deprecatedDependencies: Seq[DeprecatedDependency],
                latestDependencies:Option[Map[ModuleID, Try[Version]]]): List[Message]={


    val seq: Seq[(ModuleID, Try[Version])] = projectDependencies.map { p => p -> Try(Version(p.revision)) }

    val mandatoryMessages = checkMandatoryDependencies(deprecatedDependencies, seq.toMap)
    val repositoryMessages = latestDependencies.map { ld => calculateRepositoryResults(ld) }.getOrElse(List.empty[Message])

    mandatoryMessages ++ repositoryMessages.filterNot { m => mandatoryMessages.map(_.moduleName).contains(m.moduleName) }
  }

  def checkMandatoryDependencies(excludes: Seq[DeprecatedDependency], latestRevisions: Map[ModuleID, Try[Version]]): List[Message] = {
    latestRevisions.toList.flatMap({
      case (module, latestRevision) =>
        DependencyChecker.isDependencyValid(excludes)(Dependency(module.organization, module.name), Version(module.revision)) match {
          case MandatoryFail(exclusion) =>
            Some(new DependencyUnusable(module, latestRevision, exclusion))

          case MandatoryWarn(exclusion) =>
            Some(new DependencyNearlyUnusable(module, latestRevision, exclusion))

          case _ => None
        }
    })
  }


  def calculateRepositoryResults(latestRevisions: Map[ModuleID, Try[Version]]): List[Message] = {
    latestRevisions.toList.flatMap {
      case (module, Failure(ex)) =>
        Some(new UnknownVersion(module, ex))

      case (module, Success(latestRevision)) if latestRevision.isAfter(Version(module.revision)) =>
        Some(new NewVersionAvailable(module, Success(latestRevision)))

      case a @ _ =>
        logger.warn("in bad state, got module and version: " + a)
        None
    }
  }
}
