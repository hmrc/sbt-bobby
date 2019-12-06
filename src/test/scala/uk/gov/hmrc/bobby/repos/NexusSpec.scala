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

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sbt.ModuleID
import uk.gov.hmrc.bobby.conf.NexusCredentials
import uk.gov.hmrc.bobby.domain.Version

class NexusSpec extends AnyFlatSpec with Matchers with OptionValues {

  object NexusUnderTest extends Nexus {
    override val nexus: NexusCredentials = NexusCredentials("", "", "")
  }

  private val moduleId: ModuleID           = ModuleID("uk.gov.hmrc", "auth", "3.2.1-SNAPSHOT")
  private val scalaVersion: Option[String] = Some("2.11")

  it should "construct nexus search query parameters which in the Scala version if provided" ignore {
    NexusUnderTest.getSearchTerms(moduleId, scalaVersion) shouldBe "auth_2.11&g=uk.gov.hmrc"
  }

  it should "omit the Scala version from nexus search query if not provided" in {
    NexusUnderTest.getSearchTerms(moduleId, None) shouldBe "auth&g=uk.gov.hmrc"
  }

  "10.1.8.6.8.5" should "be shortened to 10.1" in {
    NexusUnderTest.shortenScalaVersion("10.1.8.6.8.5") shouldBe "10.1"
  }

  "2.10" should "be shortened to 2.10" in {
    NexusUnderTest.shortenScalaVersion("2.10") shouldBe "2.10"
  }

  "Nexus lucene client" should "get versions from Nexus search results" in {
    NexusUnderTest.parseVersions(xml) shouldBe Seq(Version(2, 2, 3, Some(Right("SNAP1"))), Version(2, 2, 2))
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
