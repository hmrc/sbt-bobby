/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.bobby.domain

import org.scalatest.{FlatSpec, Matchers}

class VersionSpec extends FlatSpec with Matchers {

  "1.0.0" should "be greater than 0.0.1" in {
    Version("1.0.0").isAfter(Version("0.0.1")) shouldBe true
  }

  "2.11.2" should "be greater than 2.9.3" in {
    Version("2.11.2").isAfter(Version("2.9.3")) shouldBe true
  }


  "1.0.0" should "be smaller than 1.0.1" in {
    Version("1.0.0").isBefore(Version("1.0.1")) shouldBe true
  }

  "1.0.1" should "be equal to 1.0.1" in {
    Version("1.0.1").equals(Version("1.0.1")) shouldBe true
  }
}
