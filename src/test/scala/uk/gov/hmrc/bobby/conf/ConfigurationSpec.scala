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

package uk.gov.hmrc.bobby.conf

import java.net.URL
import java.time.LocalDate

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.bobby.domain.VersionRange

class ConfigurationSpec extends AnyFlatSpec with Matchers {

  "The Configuration parser" should "read a well formatted json file with plugins and libraries and ignore anything else" in {

    val deps = Configuration.parseConfig(
      """{
        | "rules" : [
        |     { "organisation" : "uk.gov.hmrc", "name" : "some-frontend", "range" : "(,7.4.1)", "reason" : "7.4.1 has important security fixes", "from" : "2015-01-01" },
        |     { "organisation" : "uk.gov.hmrc", "name" : "some-plugin", "range" : "(,1.0.0)", "reason" : "1.0.0 is outdated", "from" : "2015-01-02" }
        | ]
        |}
      """.stripMargin)

    deps should have size 2

    deps.head.dependency.organisation shouldBe "uk.gov.hmrc"
    deps.head.dependency.name         shouldBe "some-frontend"
    deps.head.range                   shouldBe VersionRange("(,7.4.1)")
    deps.head.reason                  shouldBe "7.4.1 has important security fixes"
    deps.head.effectiveDate                    shouldBe LocalDate.of(2015, 1, 1)

    deps.last.dependency.organisation shouldBe "uk.gov.hmrc"
    deps.last.dependency.name         shouldBe "some-plugin"
    deps.last.range                   shouldBe VersionRange("(,1.0.0)")
    deps.last.reason                  shouldBe "1.0.0 is outdated"
    deps.last.effectiveDate                    shouldBe LocalDate.of(2015, 1, 2)

  }

  it should "fail-fast if all config is missing" in {
    val error = intercept[RuntimeException] {
      new Configuration().loadBobbyRules()
    }
    error.getMessage shouldBe s"Bobby rule location unknown! - Set 'deprecatedDependenciesUrl' via the config file or explicitly in the build"
  }

  it should "fail-fast if unable to retrieve the bobby rules" in {
    val error = intercept[RuntimeException] {
      new Configuration(bobbyRuleURL = Some(new URL("file://badfile"))).loadBobbyRules()
    }
    error.getMessage.startsWith("Unable to load bobby rules from") shouldBe true
  }

  "extractMap" should "return a key value map" in {
    val lines = List(
      "deprecated-dependencies = https://myurl",
      "somekey=somevalue",
      "anotherkey=http://myurl?token=mytoken",
      " key =    value  "
    )
    Configuration.extractMap(lines) shouldBe Map(
      "deprecated-dependencies" -> "https://myurl",
      "somekey" -> "somevalue",
      "anotherkey" -> "http://myurl?token=mytoken",
      "key" -> "value"
    )
  }

}
