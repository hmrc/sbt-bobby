package uk.gov.hmrc.bobby

import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.bobby.domain._

class DependencyCheckerSpec extends FlatSpec with Matchers {

  "The mandatory dependency checker" should "return success result if the version is not in a restricted range" in {

    val dc = DependencyChecker(List(Exclude(VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1))))
    dc.isVersionValid(Version("2.0.0")) shouldBe OK

  }

  it should "return failed result if the version is in a restricted range" in {

    val dc = DependencyChecker(List(Exclude(VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1))))
    dc.isVersionValid(Version("0.1.0")) shouldBe MandatoryFail(Exclude(VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1)))

  }

  it should "return failed result if the version is in a restricted range of multiple exclude" in {

    val dc = DependencyChecker(List(
      Exclude(VersionRange("(,1.0.0]"), "testing", new LocalDate().minusDays(1)),
      Exclude(VersionRange("[1.0.0,1.2.0]"), "testing", new LocalDate().minusDays(1)),
      Exclude(VersionRange("[2.0.0,2.2.0]"), "testing", new LocalDate().minusDays(1))
    ))

    dc.isVersionValid(Version("1.1.0")) shouldBe MandatoryFail(Exclude(VersionRange("[1.0.0,1.2.0]"), "testing", new LocalDate().minusDays(1)))
  }

  it should "return warning if excludes are not applicable yet" in {
    val tomorrow: LocalDate = new LocalDate().plusDays(1)
    val dc = DependencyChecker(List(Exclude(VersionRange("(,1.0.0]"), "testing", tomorrow)))
    dc.isVersionValid(Version("0.1.0")) shouldBe MandatoryWarn(Exclude(VersionRange("(,1.0.0]"), "testing", tomorrow))

  }

  it should "return fail if exclude is applicable from today" in {
    val today: LocalDate = new LocalDate()
    val dc = DependencyChecker(List(Exclude(VersionRange("(,1.0.0]"), "testing", today)))
    dc.isVersionValid(Version("0.1.0")) shouldBe MandatoryFail(Exclude(VersionRange("(,1.0.0]"), "testing", today))

  }


  it should "return failed result if the version is in both restricted warn and fail ranges" in {

    val validTomorrow: LocalDate = new LocalDate().plusDays(1)
    val validToday: LocalDate = new LocalDate().minusDays(1)
    val dc = DependencyChecker(List(
      Exclude(VersionRange("[1.0.0,1.2.0]"), "testing1", validTomorrow),
      Exclude(VersionRange("[1.0.0,2.2.0]"), "testing2", validToday)
    ))

    dc.isVersionValid(Version("1.1.0")) shouldBe MandatoryFail(Exclude(VersionRange("[1.0.0,2.2.0]"), "testing2", validToday))
  }

}
