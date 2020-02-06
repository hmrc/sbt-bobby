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

import sbt.librarymanagement.ModuleID

sealed trait BobbyResult {
  def failed: Boolean
  def rule: Option[BobbyRule]
  def name: String
}

case class BobbyViolation(r: BobbyRule) extends BobbyResult() {
  val rule: Option[BobbyRule] = Some(r)
  val failed: Boolean = true
  val name: String = "BobbyViolation"
}

case class BobbyWarning(r: BobbyRule) extends BobbyResult {
  val rule: Option[BobbyRule] = Some(r)
  val failed: Boolean = false
  val name: String = "BobbyWarning"
}

case object BobbyOk extends BobbyResult {
  val rule: Option[BobbyRule] = None
  val failed: Boolean = false
  val name: String = "BobbyOk"
}

case class BobbyChecked(moduleID: ModuleID, `type`: DependencyType, result: BobbyResult)
