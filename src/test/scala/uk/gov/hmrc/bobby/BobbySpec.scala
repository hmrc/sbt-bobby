/*
 * Copyright 2015 HM Revenue & Customs
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

import org.scalatest.{FlatSpec, Matchers}
import sbt.ModuleID
import uk.gov.hmrc.bobby.LogLevels.{ERROR, INFO, WARN}
import uk.gov.hmrc.bobby.MessageBuilder._

class BobbySpec extends FlatSpec with Matchers {

  it should "compact dependencies by using one dependnecy when more than one has the " in {
    val mods = Seq(
      ModuleID("uk.gov.hmrc", "auth", "3.2.0", configurations = Some("test")),
      ModuleID("uk.gov.hmrc", "auth", "3.2.0"))

    Bobby.compactDependencies(mods).size shouldBe 1
  }

  it should "prepare dependencies by removing any on a blacklist" in {
    val auth = ModuleID("uk.gov.hmrc","auth", "3.2.0")

    val mods = Seq(
      auth,
      ModuleID("com.typesafe.play", "play-ws", "6.2.0", None)
    )

    val blacklisted: Set[String] = Set("com.typesafe.play")

    Bobby.prepareDependencies(mods, blacklisted) shouldBe Seq(auth)
  }

  it should "order messages correctly" in {

    val messages = Seq(
      makeMessage(INFO, "info message"),
      makeMessage(ERROR, "error message"),
      makeMessage(WARN, "warn message"))

    messages.sorted(Message.MessageOrdering).map(_.level) shouldBe Seq(ERROR, WARN, INFO)
  }
}
