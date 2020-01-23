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

package uk.gov.hmrc.bobby.repos

import java.net.URL

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import uk.gov.hmrc.bobby.conf.NexusCredentials

import scala.util.Try

class NexusCredentialsSpec extends AnyFlatSpec with Matchers {

  "buildsearchUrl" should
    "build valid URL" in {
    val result = NexusCredentials("nexus.host.com", "myUsername", "somePassword").buildSearchUrl("query")
    result                        shouldBe "https://myUsername:somePassword@nexus.host.com/service/local/lucene/search?a=query"
    Try(new URL(result)).toOption shouldBe defined
  }

  it should "encode characters that may break the URL" in {
    val result = NexusCredentials("nexus.host.com", "myUsername@mydomain.com", "someP!ssword").buildSearchUrl("query")
    result                        shouldBe "https://myUsername%40mydomain.com:someP%21ssword@nexus.host.com/service/local/lucene/search?a=query"
    Try(new URL(result)).toOption shouldBe defined
  }

}
