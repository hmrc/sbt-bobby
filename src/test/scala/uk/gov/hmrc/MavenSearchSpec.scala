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
import uk.gov.hmrc.bobby.MavenSearch
import uk.gov.hmrc.bobby.domain.Version

class MavenSearchSpec extends FlatSpec with Matchers with OptionValues{

  "Maven search" should "get versions from Maven search results" in {
    MavenSearch.parseVersions(xml) shouldBe Seq(Version(3,0,0, Some(Right("M1"))), Version(2,2,4))
  }

  val xml = <response>
    <lst name="responseHeader">
      <int name="status">0</int>
      <int name="QTime">0</int>
      <lst name="params">
        <str name="fl">id,g,a,v,p,ec,timestamp,tags</str>
        <str name="sort">score desc,timestamp desc,g asc,a asc,v desc</str>
        <str name="indent">off</str>
        <str name="q">g:"org.scalatest" AND a:"scalatest_2.10"</str>
        <str name="core">gav</str>
        <str name="wt">xml</str>
        <str name="rows">20</str>
        <str name="version">2.2</str>
      </lst>
    </lst>
    <result name="response" numFound="92" start="0">
      <doc>
        <str name="a">scalatest_2.10</str>
        <arr name="ec">
          <str>-javadoc.jar</str>
          <str>-sources.jar</str>
          <str>.jar</str>
          <str>.pom</str>
        </arr>
        <str name="g">org.scalatest</str>
        <str name="id">org.scalatest:scalatest_2.10:3.0.0-M1</str>
        <str name="p">bundle</str>
        <arr name="tags">
          <str>scalatest</str>
        </arr>
        <long name="timestamp">1433861247000</long>
        <str name="v">3.0.0-M1</str>
      </doc>
      <doc>
        <str name="a">scalatest_2.10</str>
        <arr name="ec">
          <str>-javadoc.jar</str>
          <str>-sources.jar</str>
          <str>.jar</str>
          <str>.pom</str>
        </arr>
        <str name="g">org.scalatest</str>
        <str name="id">org.scalatest:scalatest_2.10:2.2.4</str>
        <str name="p">bundle</str>
        <arr name="tags">
          <str>scalatest</str>
        </arr>
        <long name="timestamp">1422519426000</long>
        <str name="v">2.2.4</str>
      </doc>
    </result>
  </response>
}
