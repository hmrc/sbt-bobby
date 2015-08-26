/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.bobby

import org.joda.time.LocalDate
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._

trait DependencyChecker {

  val logger = ConsoleLogger()

  val excludes: Seq[DeprecatedDependency]

  def isDependencyValid(dependency: Dependency, version: Version): DependencyCheckResult = {
    val filtered = excludes.filter(dd => {
      (dd.dependency.organisation.equals(dependency.organisation) || dd.dependency.organisation.equals("*")) &&
        (dd.dependency.name.equals(dependency.name) || dd.dependency.name.equals("*"))
    })

    filtered.foldLeft[DependencyCheckResult](OK) {
      case (OK, exclude) if exclude.range.includes(version) && exclude.from.isAfter(new LocalDate()) => MandatoryWarn(exclude)
      case (result: Pass, exclude) if exclude.range.includes(version) => MandatoryFail(exclude)
      case (result, exclude) => result
    }
  }
}

object DependencyChecker extends DependencyChecker {

  override val excludes: Seq[DeprecatedDependency] = Configuration.deprecatedDependencies

}
