/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.bobby.repos

import org.scalatest.{OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import sbt.ModuleID
import uk.gov.hmrc.bobby.conf.BintrayCredentials
import uk.gov.hmrc.bobby.domain.Version

import scala.util.Failure

class HmrcBintraySpec extends AnyFlatSpec with Matchers with OptionValues {

  "Bintray build search url" should "build the Bintray URL including scala version" in {
    HmrcBintray
      .buildSearchUrl(ModuleID("uk.gov.hmrc", "time", "1.2.0"), Some("2.11"))
      .toString shouldBe "https://bintray.com/artifact/download/hmrc/releases/uk/gov/hmrc/time_2.11/maven-metadata.xml"
  }

  "Bintray build search url" should "build the Bintray URL not including scala version" in {
    HmrcBintray
      .buildSearchUrl(ModuleID("uk.gov.hmrc", "time", "1.2.0"), None)
      .toString shouldBe "https://bintray.com/artifact/download/hmrc/releases/uk/gov/hmrc/time/maven-metadata.xml"
  }

  "Bintray search" should "not return search results for a non-hmrc library" in {
    HmrcBintray.search(ModuleID("non.hmrc", "x", "0.1"), None).failed.get.getMessage shouldBe "(non-hmrc)"
  }

  "Bintray search" should "get versions from Bintray search results" in {
    HmrcBintray.latestVersion(xml) shouldBe Some(Version("3.0.0"))
  }

  val xml = """<?xml version="1.0" encoding="UTF-8"?>
               |<metadata>
               |  <groupId>uk.gov.hmrc</groupId>
               |  <artifactId>play-authorisation_2.11</artifactId>
               |  <version>3.0.0</version>
               |  <versioning>
               |    <latest>3.0.0</latest>
               |    <release>3.0.0</release>
               |    <versions>
               |      <version>2.0.0</version>
               |      <version>2.1.0</version>
               |      <version>3.0.0</version>
               |    </versions>
               |    <lastUpdated>20151106120429</lastUpdated>
               |  </versioning>
               |</metadata>""".stripMargin('|')
}
