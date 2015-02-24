package uk.gov.hmrc.bobby

import org.joda.time.LocalDate
import uk.gov.hmrc.bobby.domain._

case class DependencyChecker(excludes: Seq[Exclude]) {

  def isVersionValid(version: Version): DependencyCheckResult = excludes.foldLeft[DependencyCheckResult](OK) {
    case (OK, exclude) if exclude.range.includes(version) && exclude.from.isAfter(new LocalDate()) => MandatoryWarn(exclude)
    case (result: Pass, exclude) if exclude.range.includes(version) => MandatoryFail(exclude)
    case (result, exclude) => result
  }

}

