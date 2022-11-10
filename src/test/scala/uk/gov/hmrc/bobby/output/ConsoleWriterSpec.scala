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

package uk.gov.hmrc.bobby.output

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sbt.librarymanagement.ModuleID
import uk.gov.hmrc.bobby.domain.{BobbyChecked, BobbyResult, BobbyValidationResult, Message}

class ConsoleWriterSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  "renderText" should {
    "include ansi colour codes if set" in {
      val cw = new ConsoleWriter(true)
      val messages = List(Message(BobbyChecked(ModuleID("myorg", "myname", "0.1.0"), BobbyResult.Ok), Seq.empty))
      cw.renderText(BobbyValidationResult(messages), Flat).contains(Console.GREEN) shouldBe true
    }

    "not include ansi colour codes if not set" in {
      val cw = new ConsoleWriter(false)
      val messages = List(Message(BobbyChecked(ModuleID("myorg", "myname", "0.1.0"), BobbyResult.Ok), Seq.empty))
      cw.renderText(BobbyValidationResult(messages), Flat).contains(Console.GREEN) shouldBe false
    }
  }
}
