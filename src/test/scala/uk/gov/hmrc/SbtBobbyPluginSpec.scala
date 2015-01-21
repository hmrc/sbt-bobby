package uk.gov.hmrc

import org.scalatest.{FunSpec, Matchers, FlatSpec}
import uk.gov.hmrc.Core.Version

// I recognise that this is pretty crude checking at the moment, but should be enough to get us started
class SbtBobbyPluginSpec extends FlatSpec with Matchers {

  "1.0.0" should "be greater than 0.1.0" in {
    SbtBobbyPlugin.versionIsNewer("1.0.0", "0.1.0") shouldBe true
  }

  "0.1.0" should "not be be greater than 1.0.0" in {
    SbtBobbyPlugin.versionIsNewer("0.1.0", "1.0.0") shouldBe false
  }

  "0.2.1" should "be greater than 0.2.0" in {
    SbtBobbyPlugin.versionIsNewer("1.0.0", "0.1.0") shouldBe true
  }

  "10.1.8.6.8.5" should "not be greater than 11.0" in {
    SbtBobbyPlugin.versionIsNewer("10.1.8.6.8.5", "11.0") shouldBe false
  }


  "10.1.8.6.8.5" should "be shortened to 10.1" in {
    SbtBobbyPlugin.shortenScalaVersion("10.1.8.6.8.5") shouldBe "10.1"
  }


  "2.10.1" should "be shortened to 2.10" in {
    SbtBobbyPlugin.shortenScalaVersion("2.10.1") shouldBe "2.10"
  }
}

class CoreSpec extends FunSpec with Matchers{

  it("should get versions from xml"){
    Core.versionsFromNexus(xml) shouldBe Seq(Version(Seq("2", "2", "3-SNAP1")), Version(Seq("2", "2", "2")))
  }

  it("should recognise an early release"){
    Version.isEarlyRelease(Version(Seq("2", "2", "3-SNAP1"))) shouldBe true
    Version.isEarlyRelease(Version(Seq("2", "2", "2"))) shouldBe false
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
