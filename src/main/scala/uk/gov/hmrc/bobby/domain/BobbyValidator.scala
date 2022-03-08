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

import sbt.{ConsoleLogger, ModuleID}

object BobbyValidator {

  val logger = ConsoleLogger()

  def validate(
    dependencyMap: Map[ModuleID, Seq[ModuleID]],
    dependencies: Seq[ModuleID],
    bobbyRules: List[BobbyRule],
    projectName: String
  ): BobbyValidationResult = {

    val checkedDependencies = dependencies.map(dep => BobbyChecked(dep, calc(bobbyRules, dep, projectName)))
    val messages = generateMessages(checkedDependencies, dependencyMap)

    BobbyValidationResult(messages)
  }

  def calc(
    bobbyRules: List[BobbyRule],
    dep: ModuleID,
    projectName: String,
    now: LocalDate = LocalDate.now()
  ): BobbyResult = {
    val version =
      Version(dep.revision)

    val matchingRules =
      bobbyRules
        .filter { r =>
          (r.dependency.organisation.equals(dep.organization) || r.dependency.organisation.equals("*")) &&
            (r.dependency.name.equals(dep.name) || r.dependency.name.equals("*")) &&
            r.range.includes(version)
        }
        .sorted

      matchingRules
        .map { rule =>
          if (rule.exemptProjects.contains(projectName))
            BobbyExemption(rule)
          else if (rule.effectiveDate.isBefore(now) || rule.effectiveDate.isEqual(now))
            BobbyViolation(rule)
          else
            BobbyWarning(rule)
        }
        .sorted
        .headOption
        .getOrElse(BobbyOk)
  }

  private def generateMessages(bobbyChecked: Seq[BobbyChecked], dependencyMap: Map[ModuleID, Seq[ModuleID]]): List[Message] =
    bobbyChecked
      .map(bc => Message(bc, dependencyMap.getOrElse(bc.moduleID, Seq.empty)))
      .toList
}

sealed abstract case class BobbyValidationResult(
  allMessages: List[Message],
  violations: List[Message],
  warnings: List[Message],
  exemptions: List[Message]
) {
  lazy val hasViolations: Boolean =
    violations.nonEmpty

  lazy val hasWarnings: Boolean =
    warnings.nonEmpty

  lazy val hasExemptions: Boolean =
    exemptions.nonEmpty

  lazy val hasNoIssues: Boolean =
    !(hasViolations || hasWarnings || hasExemptions)
}

object BobbyValidationResult {

  def apply(messages: List[Message]): BobbyValidationResult = {
    val all =
      messages.sortBy(_.moduleName)

    val (violations, warnings, exemptions) = {
      val byResult =
        all.groupBy(_.checked.result.name)

      (
        byResult.getOrElse("BobbyViolation", List.empty),
        byResult.getOrElse("BobbyWarning", List.empty),
        byResult.getOrElse("BobbyExemption", List.empty)
      )
    }

    new BobbyValidationResult(all, violations, warnings, exemptions) {}
  }
}
