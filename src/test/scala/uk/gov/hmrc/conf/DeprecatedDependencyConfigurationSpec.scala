package uk.gov.hmrc.conf

import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.bobby.conf.DeprecatedDependencyConfiguration
import uk.gov.hmrc.bobby.domain.VersionRange

class DeprecatedDependencyConfigurationSpec extends FlatSpec with Matchers {

  "The Configuration parser" should "read a well formatted json file with one element" in {

    val c = DeprecatedDependencyConfiguration(
      """
        |[
        |   { "organisation" : "uk.gov.hmrc", "name" : "some-frontend", "range" : "(,7.4.1)", "reason" : "7.4.1 has important security fixes", "from" : "2015-01-01" }
        |]
      """.stripMargin)

    c.head.dependency.organisation shouldBe "uk.gov.hmrc"
    c.head.dependency.name shouldBe "some-frontend"
    c.head.range shouldBe VersionRange("(,7.4.1)")
    c.head.reason shouldBe "7.4.1 has important security fixes"
    c.head.from shouldBe new LocalDate(2015, 01, 01)

  }

  it should "read a well formatted json file with multiple elements" in {

    val c = DeprecatedDependencyConfiguration(
      """
        |[
        |   { "organisation" : "uk.gov.hmrc", "name" : "some-frontend", "range" : "(,7.4.1)", "reason" : "7.4.1 has important security fixes", "from" : "2015-01-01" },
        |   { "organisation" : "uk.gov.hmrc", "name" : "some-service", "range" : "[8.0.0, 8.4.1]", "reason" : "Versions between 8.0.0 and 8.4.1 have a bug", "from" : "2015-03-01" }
        |]
      """.stripMargin)


    c(0).dependency.organisation shouldBe "uk.gov.hmrc"
    c(0).dependency.name shouldBe "some-frontend"
    c(0).range shouldBe VersionRange("(,7.4.1)")
    c(0).reason shouldBe "7.4.1 has important security fixes"
    c(0).from shouldBe new LocalDate(2015, 01, 01)


    c(1).dependency.organisation shouldBe "uk.gov.hmrc"
    c(1).dependency.name shouldBe "some-service"
    c(1).range shouldBe VersionRange("[8.0.0, 8.4.1]")
    c(1).reason shouldBe "Versions between 8.0.0 and 8.4.1 have a bug"
    c(1).from shouldBe new LocalDate(2015, 03, 01)

  }


}
