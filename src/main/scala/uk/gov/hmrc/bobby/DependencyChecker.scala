package uk.gov.hmrc.bobby

import org.joda.time.LocalDate
import uk.gov.hmrc.bobby.domain._

case class DependencyChecker(excludes: Seq[DeprecatedDependency]) {

  def isDependencyValid(dependency: Dependency, version: Version): DependencyCheckResult = {
    val filtered = excludes.filter(dd => dd.dependency.organisation.equals(dependency.organisation) && dd.dependency.name.equals(dependency.name))

    filtered.foldLeft[DependencyCheckResult](OK) {
      case (OK, exclude) if exclude.range.includes(version) && exclude.from.isAfter(new LocalDate()) => MandatoryWarn(exclude)
      case (result: Pass, exclude) if exclude.range.includes(version) => MandatoryFail(exclude)
      case (result, exclude) => result
    }
  }
}

