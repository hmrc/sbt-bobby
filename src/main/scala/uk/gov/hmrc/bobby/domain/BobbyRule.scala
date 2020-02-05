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

sealed trait DependencyType

object DependencyType {

  def apply(s: String): DependencyType = s match {
    case "plugins"   => Plugin
    case "libraries" => Library
    case _           => Unknown
  }

}

case object Library extends DependencyType
case object Plugin extends DependencyType
case object Unknown extends DependencyType

case class BobbyRule(
  dependency: Dependency,
  range: VersionRange,
  reason: String,
  effectiveDate: LocalDate,
  `type`: DependencyType)

case class BobbyRules(rules: List[BobbyRule] = List.empty) {
  lazy val (plugins, libs) = rules.partition(_.`type` == Plugin)
}
