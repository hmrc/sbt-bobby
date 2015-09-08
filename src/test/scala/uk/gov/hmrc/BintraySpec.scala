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

package uk.gov.hmrc

import org.scalatest.{FlatSpec, Matchers, OptionValues}
import sbt.ModuleID
import uk.gov.hmrc.bobby.Bintray
import uk.gov.hmrc.bobby.conf.BintrayCredentials


class BintraySpec extends FlatSpec with Matchers with OptionValues{

  "Bintray build search url" should "build the Bintray URL" in {
    new Bintray{
      override val bintrayCred: BintrayCredentials = new BintrayCredentials("foo", "bar")
    }.buildSearchUrl(ModuleID("uk.gov.hmrc", "time", "1.2.0"), None).toString shouldBe "https://bintray.com/api/v1/search/packages?subject=hmrc&repo=releases&name=time"
  }

  "Bintray search" should "get versions from Bintray search results" in {
    new Bintray{
      override val bintrayCred: BintrayCredentials = new BintrayCredentials("foo", "bar")
    }.latestVersion(json,"sbt-bobby") shouldBe Some("0.13.0")
  }

  val json = """[
               |{
               |      "name":"sbt-bobby-new",
               |      "repo":"sbt-plugin-releases",
               |      "owner":"hmrc",
               |      "desc":"",
               |      "labels":[
               |
               |      ],
               |      "attribute_names":[
               |
               |      ],
               |      "licenses":[
               |         "Apache-2.0"
               |      ],
               |      "custom_licenses":[
               |
               |      ],
               |      "followers_count":0,
               |      "created":"2015-03-17T15:20:08.150Z",
               |      "website_url":"",
               |      "issue_tracker_url":"",
               |      "linked_to_repos":[
               |
               |      ],
               |      "permissions":[
               |
               |      ],
               |      "versions":[
               |         "0.3.0",
               |         "0.1.2"
               |      ],
               |      "latest_version":"0.3.0",
               |      "updated":"2015-08-24T10:10:02.871Z",
               |      "rating_count":0,
               |      "system_ids":[
               |
               |      ],
               |      "vcs_url":"https://github.com/hmrc/sbt-bobby"
               |   },
               |   {
               |      "name":"sbt-bobby",
               |      "repo":"sbt-plugin-releases",
               |      "owner":"hmrc",
               |      "desc":"",
               |      "labels":[
               |
               |      ],
               |      "attribute_names":[
               |
               |      ],
               |      "licenses":[
               |         "Apache-2.0"
               |      ],
               |      "custom_licenses":[
               |
               |      ],
               |      "followers_count":0,
               |      "created":"2015-03-17T15:20:08.150Z",
               |      "website_url":"",
               |      "issue_tracker_url":"",
               |      "linked_to_repos":[
               |
               |      ],
               |      "permissions":[
               |
               |      ],
               |      "versions":[
               |         "0.13.0",
               |         "0.10.2",
               |         "0.12.0",
               |         "0.11.0",
               |         "0.7.0",
               |         "0.8.5",
               |         "0.9.0",
               |         "0.9.2",
               |         "0.8.3",
               |         "0.10.1",
               |         "0.10.0",
               |         "0.1.0",
               |         "0.8.0",
               |         "0.8.2",
               |         "0.8.1",
               |         "0.8.4"
               |      ],
               |      "latest_version":"0.13.0",
               |      "updated":"2015-08-24T10:10:02.871Z",
               |      "rating_count":0,
               |      "system_ids":[
               |
               |      ],
               |      "vcs_url":"https://github.com/hmrc/sbt-bobby"
               |   }
               |]""".stripMargin('|')
}
