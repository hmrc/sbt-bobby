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
    val checkedDependencies =
      dependencies.map(dep => BobbyChecked(dep, calc(bobbyRules, dep, projectName)))

    val messages =
      checkedDependencies
        .map(bc => Message(bc, dependencyMap.getOrElse(bc.moduleID, Seq.empty)))
        .toList

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
}

sealed trait BobbyValidationResult {
  def allMessages: List[Message]
  def violations: List[Message]
  def warnings: List[Message]
  def exemptions: List[Message]
  def hasViolations: Boolean
  def hasWarnings: Boolean
  def hasExemptions: Boolean
  def hasNoIssues: Boolean
}

object BobbyValidationResult {

  def apply(messages: List[Message]): BobbyValidationResult =
    Impl(messages.sortBy(_.moduleName))

  private final case class Impl(allMessages: List[Message]) extends BobbyValidationResult {

    override lazy val violations: List[Message] =
      byResultName
        .getOrElse(BobbyViolation.tag, List.empty)

    override lazy val warnings: List[Message] =
      byResultName
        .getOrElse(BobbyWarning.tag, List.empty)

    override lazy val exemptions: List[Message] =
      byResultName
        .getOrElse(BobbyExemption.tag, List.empty)

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
        .groupBy(_.checked.result.name)
  }
}