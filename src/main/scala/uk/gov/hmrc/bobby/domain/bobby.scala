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
  import BobbyResult._

  def rule: Option[BobbyRule]

  final def name: String =
    this match {
      case Violation(_) => Violation.tag
      case Warning(_)   => Warning.tag
      case Exemption(_) => Exemption.tag
      case Ok           => Ok.tag
    }

  final protected def ordering: Int =
    this match {
      case Violation(_) => 0
      case Warning(_)   => 1
      case Exemption(_) => 2
      case Ok           => 3
    }
}

object BobbyResult {

  implicit val ordering: Ordering[BobbyResult] =
    Ordering.by(_.ordering)

  case class Violation(r: BobbyRule) extends BobbyResult() {
    val rule: Option[BobbyRule] = Some(r)
  }

  object Violation {
    val tag: String = "BobbyViolation"
  }

  case class Warning(r: BobbyRule) extends BobbyResult {
    val rule: Option[BobbyRule] = Some(r)
  }

  object Warning {
    val tag: String = "BobbyWarning"
  }

  case class Exemption(r: BobbyRule) extends BobbyResult {
    val rule: Option[BobbyRule] = Some(r)
  }

  object Exemption {
    val tag: String = "BobbyExemption"
  }

  case object Ok extends BobbyResult {
    val rule: Option[BobbyRule] = None
    val tag: String = "BobbyOk"
  }
}

case class BobbyChecked(moduleID: ModuleID, result: BobbyResult)
