package uk.gov.hmrc

import org.scalatest.{Matchers, FlatSpec}

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


  "10" should "be shortened to 10" in {
    SbtBobbyPlugin.shortenScalaVersion("10") shouldBe "10"
  }

}
