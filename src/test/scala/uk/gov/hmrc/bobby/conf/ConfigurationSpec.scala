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

package uk.gov.hmrc.bobby.conf

import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain.VersionRange

class ConfigurationSpec extends FlatSpec with Matchers {

  "The Configuration parser" should "read a well formatted json file with one element" in {

    val c = Configuration(
      """
        |[
        |   { "organisation" : "uk.gov.hmrc", "name" : "some-frontend", "range" : "(,7.4.1)", "reason" : "7.4.1 has important security fixes", "from" : "2015-01-01" }
        |]
      """.stripMargin)

    c.head.dependency.organisation shouldBe "uk.gov.hmrc"
    c.head.dependency.name shouldBe "some-frontend"
    c.head.range shouldBe VersionRange("(,7.4.1)")
    c.head.reason shouldBe "7.4.1 has important security fixes"
    c.head.from shouldBe new LocalDate(2015, 1, 1)

  }

  it should "read a well formatted json file with multiple elements" in {

    val c = Configuration(
      """
        |[
        |   { "organisation" : "uk.gov.hmrc", "name" : "some-frontend", "range" : "(,7.4.1)", "reason" : "7.4.1 has important security fixes", "from" : "2015-01-01" },
        |   { "organisation" : "uk.gov.hmrc", "name" : "some-service", "range" : "[8.0.0, 8.4.1]", "reason" : "Versions between 8.0.0 and 8.4.1 have a bug", "from" : "2015-03-01" }
        |]
      """.stripMargin)


    c(0).dependency.organisation shouldBe "uk.gov.hmrc"
    c(0).dependency.name shouldBe "some-frontend"
    c(0).range shouldBe VersionRange("(,7.4.1)")
    c(0).reason shouldBe "7.4.1 has important security fixes"
    c(0).from shouldBe new LocalDate(2015, 1, 1)


    c(1).dependency.organisation shouldBe "uk.gov.hmrc"
    c(1).dependency.name shouldBe "some-service"
    c(1).range shouldBe VersionRange("[8.0.0, 8.4.1]")
    c(1).reason shouldBe "Versions between 8.0.0 and 8.4.1 have a bug"
    c(1).from shouldBe new LocalDate(2015, 3, 1)

  }


}
