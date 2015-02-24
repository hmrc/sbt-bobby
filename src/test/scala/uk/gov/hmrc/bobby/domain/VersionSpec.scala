package uk.gov.hmrc.bobby.domain

import org.scalatest.{FlatSpec, Matchers}

class VersionSpec extends FlatSpec with Matchers {

  "1.0.0" should "be greater than 0.0.1" in {
    Version("1.0.0").isAfter(Version("0.0.1")) shouldBe true
  }

  "1.0.0" should "be smaller than 1.0.1" in {
    Version("1.0.0").isBefore(Version("1.0.1")) shouldBe true
  }

  "1.0.1" should "be equal to 1.0.1" in {
    Version("1.0.1").equals(Version("1.0.1")) shouldBe true
  }
}
