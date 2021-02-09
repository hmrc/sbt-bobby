/*
 * Copyright 2021 HM Revenue & Customs
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

  def applyBobbyRules(
    dependencyMap: Map[ModuleID, Seq[ModuleID]],
    dependencies: Seq[ModuleID],
    bobbyRules: List[BobbyRule]): List[Message] = {

    val checkedDependencies = dependencies.map(dep => BobbyChecked(dep, calc(bobbyRules, dep)))
    val messages = generateMessages(bobbyRules, checkedDependencies, dependencyMap)

    messages.sortBy(_.moduleName).toList
  }

  def calc(bobbyRules: List[BobbyRule], dep: ModuleID, now: LocalDate = LocalDate.now()): BobbyResult = {

    val version = Version(dep.revision)

    // First sort the rules according to the precedence we impose, and partition to warnings and violations
    val (violations, warnings) = bobbyRules.sorted.partition(r => r.effectiveDate.isBefore(now) || r.effectiveDate.equals(now))

    def matching(rules: List[BobbyRule]): List[BobbyRule] = rules.filter { rule =>
      (rule.dependency.organisation.equals(dep.organization) || rule.dependency.organisation.equals("*")) &&
        (rule.dependency.name.equals(dep.name) || rule.dependency.name.equals("*"))
    }.filter(_.range.includes(version))

    // Always consider violations before warnings
    matching(violations).headOption.map(BobbyViolation)
      .orElse(matching(warnings).headOption.map(BobbyWarning))
      .getOrElse(BobbyOk)

  }

  def generateMessages(rules: Seq[BobbyRule], bobbyChecked: Seq[BobbyChecked],
                       dependencyMap: Map[ModuleID, Seq[ModuleID]] = Map.empty): Seq[Message] = {
    bobbyChecked.map ( bc => Message(bc, dependencyMap.getOrElse(bc.moduleID, Seq.empty)) )
  }
}
