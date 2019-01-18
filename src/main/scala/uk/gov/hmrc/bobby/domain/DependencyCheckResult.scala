/*
 * Copyright 2019 HM Revenue & Customs
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

sealed trait DependencyCheckResult {
  def fail: Boolean
}

trait Fail extends DependencyCheckResult { val fail = true }
trait Pass extends DependencyCheckResult { val fail = false }

case class MandatoryFail(exclusion: DeprecatedDependency) extends DependencyCheckResult with Fail
case class MandatoryWarn(exclusion: DeprecatedDependency) extends DependencyCheckResult with Pass
case class NexusHasNewer(latest: String) extends DependencyCheckResult with Pass
object NotFoundInNexus extends DependencyCheckResult with Pass
object OK extends DependencyCheckResult with Pass
