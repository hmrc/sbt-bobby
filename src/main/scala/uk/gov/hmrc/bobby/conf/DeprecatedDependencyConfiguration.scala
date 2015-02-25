package uk.gov.hmrc.bobby.conf

import java.net.URL

import play.api.libs.json.Json
import uk.gov.hmrc.bobby.domain.DeprecatedDependency

import scala.io.Source

object DeprecatedDependencyConfiguration {

  def apply(url: URL): Seq[DeprecatedDependency] = this(Source.fromURL(url).mkString)
  def apply(jsonConfig: String): Seq[DeprecatedDependency] = Json.parse(jsonConfig).as[Seq[DeprecatedDependency]]
}
