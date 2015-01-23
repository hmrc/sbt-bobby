package uk.gov.hmrc

import org.scalatest.{FunSpec, Matchers, FlatSpec}
import uk.gov.hmrc.Core.{OrganizationName, Version}

// I recognise that this is pretty crude checking at the moment, but should be enough to get us started
class SbtBobbyPluginSpec extends FlatSpec with Matchers {

  "10.1.8.6.8.5" should "be shortened to 10.1" in {
    SbtBobbyPlugin.shortenScalaVersion("10.1.8.6.8.5") shouldBe "10.1"
  }

  "2.10" should "be shortened to 2.10" in {
    SbtBobbyPlugin.shortenScalaVersion("2.10") shouldBe "2.10"
  }
}

class CoreSpec extends FunSpec with Matchers{

  it("should read mandatory versions file"){
    val mandatoryVersions = Core.getMandatoryVersions(this.getClass.getClassLoader.getResource("mandatory-example.txt"))

    mandatoryVersions(OrganizationName("org.scala-lang", "scala-library")) shouldBe "2.11.2"
    mandatoryVersions(OrganizationName("uk.gov.hmrc", "play-health")) shouldBe "0.8.0"
  }

  it("should get versions from Nexus search results"){
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
