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