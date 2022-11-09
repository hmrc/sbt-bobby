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

import sbt.librarymanagement.ModuleID

sealed trait BobbyResult {
  def rule: Option[BobbyRule]

  final def name: String =
    this match {
      case BobbyViolation(_) => BobbyViolation.tag
      case BobbyWarning(_)   => BobbyWarning.tag
      case BobbyExemption(_) => BobbyExemption.tag
      case BobbyOk           => BobbyOk.tag
    }

  final protected def ordering: Int =
    this match {
      case BobbyViolation(_) => 0
      case BobbyWarning(_)   => 1
      case BobbyExemption(_) => 2
      case BobbyOk           => 3
    }
}

object BobbyResult {

  implicit val ordering: Ordering[BobbyResult] =
    Ordering.by(_.ordering)
}

// TODO move the following into BobbyResult companion object

case class BobbyViolation(r: BobbyRule) extends BobbyResult() {
  val rule: Option[BobbyRule] = Some(r)
}

object BobbyViolation {
  val tag: String = "BobbyViolation"
}

case class BobbyWarning(r: BobbyRule) extends BobbyResult {
  val rule: Option[BobbyRule] = Some(r)
}

object BobbyWarning {
  val tag: String = "BobbyWarning"
}

case class BobbyExemption(r: BobbyRule) extends BobbyResult {
  val rule: Option[BobbyRule] = Some(r)
}

object BobbyExemption {
  val tag: String = "BobbyExemption"
}

case object BobbyOk extends BobbyResult {
  val rule: Option[BobbyRule] = None
  val tag: String = "BobbyOk"
}

case class BobbyChecked(moduleID: ModuleID, result: BobbyResult)
