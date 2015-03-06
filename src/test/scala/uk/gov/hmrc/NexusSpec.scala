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
package uk.gov.hmrc

import org.scalatest.{FlatSpec, FunSpec, Matchers, OptionValues}
import uk.gov.hmrc.bobby.Nexus
import uk.gov.hmrc.bobby.domain.Version

class NexusSpec extends FlatSpec with Matchers with OptionValues{

  "10.1.8.6.8.5" should "be shortened to 10.1" in {
    Nexus.shortenScalaVersion("10.1.8.6.8.5") shouldBe "10.1"
  }

  "2.10" should "be shortened to 2.10" in {
    Nexus.shortenScalaVersion("2.10") shouldBe "2.10"
  }

  "Nexus lucene client" should "get versions from Nexus search results" in {
    Nexus.versionsFromNexus(xml) shouldBe Seq(Version(2,2,3, Some(Right("SNAP1"))), Version(2,2,2))
  }

  val xml = <searchNGResponse>
    <totalCount>65</totalCount>
    <from>-1</from>
    <count>-1</count>
    <tooManyResults>false</tooManyResults>
    <collapsed>false</collapsed>
    <repoDetails/>
    <data>
      <artifact>
        <groupId>org.scalatest</groupId>
        <artifactId>scalatest_2.11</artifactId>
        <version>2.2.3-SNAP1</version>
        <latestRelease>2.2.3-SNAP1</latestRelease>
        <latestReleaseRepositoryId>sonatype-oss-releases</latestReleaseRepositoryId>
        <artifactHits>
          <artifactHit>
            <repositoryId>sonatype-oss-releases</repositoryId>
            <artifactLinks>
              <artifactLink>
                <extension>pom</extension>
              </artifactLink>
              <artifactLink>
                <extension>jar</extension>
              </artifactLink>
              <artifactLink>
                <classifier>sources</classifier>
                <extension>jar</extension>
              </artifactLink>
              <artifactLink>
                <classifier>javadoc</classifier>
                <extension>jar</extension>
              </artifactLink>
            </artifactLinks>
          </artifactHit>
        </artifactHits>
      </artifact>
      <artifact>
        <groupId>org.scalatest</groupId>
        <artifactId>scalatest_2.11</artifactId>
        <version>2.2.2</version>
        <latestRelease>2.2.3-SNAP1</latestRelease>
        <latestReleaseRepositoryId>sonatype-oss-releases</latestReleaseRepositoryId>
        <artifactHits>
          <artifactHit>
            <repositoryId>sonatype-oss-releases</repositoryId>
            <artifactLinks>
              <artifactLink>
                <extension>pom</extension>
              </artifactLink>
              <artifactLink>
                <extension>jar</extension>
              </artifactLink>
              <artifactLink>
                <classifier>sources</classifier>
                <extension>jar</extension>
              </artifactLink>
              <artifactLink>
                <classifier>javadoc</classifier>
                <extension>jar</extension>
              </artifactLink>
            </artifactLinks>
          </artifactHit>
        </artifactHits>
      </artifact>
    </data>
    </searchNGResponse>
}
