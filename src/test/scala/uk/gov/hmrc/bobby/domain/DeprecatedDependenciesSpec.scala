/*
 * Copyright 2016 HM Revenue & Customs
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
import org.scalatest.{Matchers, FlatSpec, WordSpec}

class DeprecatedDependenciesSpec extends FlatSpec with Matchers{


  "DeprecatedDependencies" should "filter plugin and lib dependencies" in {

    val now = new LocalDate()
    val dependencies: List[DeprecatedDependency] = List(
      DeprecatedDependency(Dependency("uk.gov.hmrc", "some-service"), VersionRange("(,1.0.0]"), "testing", now, Library),
      DeprecatedDependency(Dependency("uk.gov.hmrc", "some-service"), VersionRange("(,1.0.0]"), "testing", now, Library),
      DeprecatedDependency(Dependency("uk.gov.hmrc", "some-service"), VersionRange("(,1.0.0]"), "testing", now, Plugin)
    )
    val deps = DeprecatedDependencies(dependencies)

    deps.libs should be(dependencies.take(2))
    deps.plugins should be(dependencies.takeRight(1))
  }

}
