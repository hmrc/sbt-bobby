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

package uk.gov.hmrc.bobby.domain

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DeprecatedDependency(dependency: Dependency, range: VersionRange, reason: String, from: LocalDate)

object DeprecatedDependency {

  implicit val r: Reads[DeprecatedDependency] = (
    (__ \ "organisation").read[String] and
      (__ \ "name").read[String] and
      (__ \ "range").read[String] and
      (__ \ "reason").read[String] and
      (__ \ "from").read[LocalDate]
    )((o, n, ra, re, f) => DeprecatedDependency.apply(Dependency(o, n), VersionRange(ra), re, f))
}
