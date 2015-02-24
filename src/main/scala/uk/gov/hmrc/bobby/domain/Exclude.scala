package uk.gov.hmrc.bobby.domain

import org.joda.time.LocalDate
import play.api.libs.json.{JsPath, Reads, Json}
import play.api.libs.functional.syntax._

case class Exclude(range: VersionRange, reason: String, from: LocalDate)

object Exclude {

  implicit val r: Reads[Exclude] = (
    (JsPath \ "range").read[String] and
      (JsPath \ "reason").read[String] and
      (JsPath \ "from").read[LocalDate]
    )((a, b, c) => Exclude.apply(VersionRange(a), b, c))
}