/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate

import sbt.ModuleID
import uk.gov.hmrc.graph.DependencyGraphParser

object BobbyValidator {

  def validate(
    content            : String,
    scope              : String,
    bobbyRules         : List[BobbyRule],
    internalModuleNodes: Seq[ModuleID],
    projectName        : String
  ): Seq[Message] = {
    import uk.gov.hmrc.bobby.Util._
    val graph        = DependencyGraphParser.parse(content)
    val dependencies = graph.dependencies
                         // we don't want to validate the root project or dependencies on other internal modules - will lead to SNAPSHOT violations
                         .filterNot(_ == graph.root)
                         .filterNot { n1 =>
                           internalModuleNodes.exists(n2 => n1.group == n2.organization && n1.artefact == n2.name)
                         }

    dependencies.map { dependency =>
      val result = BobbyValidator.calc(bobbyRules, dependency.toModuleID, projectName)

      Message(
        moduleID        = dependency.toModuleID,
        result          = result,
        scope           = scope,
        dependencyChain = graph.pathToRoot(dependency).map(_.toModuleID).dropRight(1) // last one is the project itself
      )
    }
  }

  def calc(
    bobbyRules : List[BobbyRule],
    dep        : ModuleID,
    projectName: String,
    now        : LocalDate = LocalDate.now()
  ): BobbyResult = {
    val version =
      Version(dep.revision)

    val matchingRules =
      bobbyRules
        .filter { rule =>
          (rule.dependency.organisation == dep.organization || rule.dependency.organisation == "*") &&
            (rule.dependency.name == dep.name || rule.dependency.name == "*") &&
            rule.range.includes(version)
        }
        .sorted

      matchingRules
        .map { rule =>
          if (rule.exemptProjects.contains(projectName))
            BobbyResult.Exemption(rule): BobbyResult
          else if (rule.effectiveDate.isBefore(now) || rule.effectiveDate.isEqual(now))
            BobbyResult.Violation(rule)
          else
            BobbyResult.Warning(rule)
        }
        .sorted
        .headOption
        .getOrElse(BobbyResult.Ok)
  }
}

sealed trait BobbyValidationResult {
  def allMessages  : List[Message]
  def violations   : List[Message]
  def warnings     : List[Message]
  def exemptions   : List[Message]
  def hasViolations: Boolean
  def hasWarnings  : Boolean
  def hasExemptions: Boolean
  def hasNoIssues  : Boolean
}

object BobbyValidationResult {
  def apply(messages: Seq[Message]): BobbyValidationResult =
    Impl(messages.toList.sortBy(_.moduleID.toString))

  private final case class Impl(allMessages: List[Message]) extends BobbyValidationResult {

    override lazy val violations: List[Message] =
      byResultName
        .getOrElse(BobbyResult.Violation.tag, List.empty)

    override lazy val warnings: List[Message] =
      byResultName
        .getOrElse(BobbyResult.Warning.tag, List.empty)

    override lazy val exemptions: List[Message] =
      byResultName
        .getOrElse(BobbyResult.Exemption.tag, List.empty)

    override lazy val hasViolations: Boolean =
      violations.nonEmpty

    override lazy val hasWarnings: Boolean =
      warnings.nonEmpty

    override lazy val hasExemptions: Boolean =
      exemptions.nonEmpty

    override lazy val hasNoIssues: Boolean =
      !(hasViolations || hasWarnings || hasExemptions)

    private lazy val byResultName =
      allMessages
        .groupBy(_.result.name)
  }
}
