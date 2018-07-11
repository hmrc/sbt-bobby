/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.bobby.domain

import org.joda.time.LocalDate
import sbt.ConsoleLogger

object DependencyChecker {

  val logger = ConsoleLogger()

  def isDependencyValid(excludes: Seq[DeprecatedDependency])(dependency: Dependency, version: Version): DependencyCheckResult = {
    val filtered = excludes.filter(dd => {
      (dd.dependency.organisation.equals(dependency.organisation) || dd.dependency.organisation.equals("*")) &&
        (dd.dependency.name.equals(dependency.name) || dd.dependency.name.equals("*"))
    })

    val now = new LocalDate()

    val fails = filtered.filter { exclude =>
      exclude.range.includes(version) && (exclude.from.isBefore(now) || exclude.from.isEqual(now))
    }.sortBy(_.from.toDate)

    val warns = filtered.filter { exclude =>
      exclude.range.includes(version) && exclude.from.isAfter(now)
    }.sortBy(_.from.toDate)

    val failO = fails.headOption.map { dep => MandatoryFail(dep) }

    val warnO = warns.headOption.map { dep => MandatoryWarn(dep) }

    (failO, warnO) match {
      case(Some(fail), _)    => fail
      case(None, Some(warn)) => warn
      case(None, None)       => OK
    }
  }
}
