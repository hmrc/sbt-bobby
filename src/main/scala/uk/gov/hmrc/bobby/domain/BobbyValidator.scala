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

import java.time.LocalDate

import sbt.{ConsoleLogger, ModuleID}

object BobbyValidator {

  val logger = ConsoleLogger()

  def applyBobbyRules(
     projectDependencyMap: Map[ModuleID, Seq[ModuleID]],
     projectDependencies: Seq[ModuleID],
     pluginDependencies: Seq[ModuleID],
     bobbyRules: BobbyRules): List[Message] = {

    val checkedProjectDependencies = projectDependencies.map(dep => BobbyChecked(dep, Library, calc(bobbyRules.libs, dep)))
    val checkedPluginDependencies = pluginDependencies.map(dep => BobbyChecked(dep, Plugin, calc(bobbyRules.plugins, dep)))

    val projectMessages = generateMessages(bobbyRules.libs, checkedProjectDependencies, projectDependencyMap)
    val pluginMessages = generateMessages(bobbyRules.plugins, checkedPluginDependencies)

    (projectMessages ++ pluginMessages).sortBy(_.moduleName).toList

  }

  def calc(bobbyRules: List[BobbyRule], dep: ModuleID, now: LocalDate = LocalDate.now()): BobbyResult = {

    val version = Version(dep.revision)

    val matchingRules = bobbyRules.filter{ rule =>
      (rule.dependency.organisation.equals(dep.organization) || rule.dependency.organisation.equals("*")) &&
        (rule.dependency.name.equals(dep.name) || rule.dependency.name.equals("*"))
    }.filter(_.range.includes(version))

    // Apply the earliest enforced rule first in the case of multiple
    matchingRules.sortWith((a, b) => a.effectiveDate.isBefore(b.effectiveDate)).headOption  match {
      case Some(rule) =>
        rule.effectiveDate match {
          case d if d.isBefore(now) || d.isEqual(now) => BobbyViolation(rule)
          case _ => BobbyWarning(rule)
        }
      case _ => BobbyOk
    }

  }

  def generateMessages(rules: Seq[BobbyRule], bobbyChecked: Seq[BobbyChecked],
                       dependencyMap: Map[ModuleID, Seq[ModuleID]] = Map.empty): Seq[Message] = {
    bobbyChecked.map ( bc => Message(bc, dependencyMap.getOrElse(bc.moduleID, Seq.empty)) )
  }
}
