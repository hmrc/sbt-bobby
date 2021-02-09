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

package uk.gov.hmrc.bobby

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.bobby.domain.MessageBuilder._
import uk.gov.hmrc.bobby.domain.MessageLevels.{ERROR, INFO, WARN}
import uk.gov.hmrc.bobby.domain._

import java.time.LocalDate

class BobbySpec extends AnyFlatSpec with Matchers {

  it should "order messages correctly" in {
    val rule = BobbyRule(Dependency("uk.gov.hmrc", "auth"), VersionRange("(,3.0.0]"), "testing", LocalDate.now())

    val messages = Seq(
      makeMessage(BobbyOk),
      makeMessage(BobbyWarning(rule)),
      makeMessage(BobbyViolation(rule)))

    messages.sorted(Message.MessageOrdering).map(_.level) shouldBe Seq(ERROR, WARN, INFO)
  }
}
