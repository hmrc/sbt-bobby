/*
 * Copyright 2016 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

class ApplicationConfigSpec extends FlatSpec with Matchers {

  "Assets frontend Prod version" should "output nothing if it doesn't exist" in {
    val configWithoutAssets = ConfigFactory.parseString("")
    val applicationConfig = new ApplicationConfig(configWithoutAssets)

    applicationConfig.assetsFrontendVersion shouldBe None
  }

  it should "output nothing when 'assets' object is at the root" in {
    val configWithAssets = ConfigFactory.parseString(
      """assets {
        |  version = "2.135.0"
        |}""".stripMargin)
    val applicationConfig = new ApplicationConfig(configWithAssets)

    applicationConfig.assetsFrontendVersion.get shouldBe "2.135.0"
  }

  it should "output nothing when 'assets' object is nested" in {
    val configWithAssetsInGovUkBlock = ConfigFactory.parseString(
      """govuk-tax {
        |  Prod {
        |    assets {
        |      version = "1.225.0"
        |    }
        |  }
        |}""".stripMargin)
    val applicationConfig = new ApplicationConfig(configWithAssetsInGovUkBlock)

    applicationConfig.assetsFrontendVersion shouldBe None
  }

  it should "output '1.339.0'" in {
    val configWithAssets = ConfigFactory.parseString(
      """Prod {
        |  assets {
        |    version = "1.339.0"
        |  }
        |}""".stripMargin)
    val applicationConfig = new ApplicationConfig(configWithAssets)

    applicationConfig.assetsFrontendVersion shouldBe None
  }
}
