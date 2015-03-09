/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.bobby.conf

import java.net.URL

import play.api.libs.json.Json
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain.DeprecatedDependency

import scala.io.Source

object DeprecatedDependencyConfiguration {

  val timeout = 3000
  val logger = ConsoleLogger()

  def apply(url: URL): Seq[DeprecatedDependency] = {
    try {

      val conn = url.openConnection()
      conn.setConnectTimeout(timeout)
      conn.setReadTimeout(timeout)
      val inputStream = conn.getInputStream


      this(Source.fromInputStream(inputStream).mkString)
    } catch {
      case e: Exception =>
        logger.warn(s"[bobby] Unable load configuration from ${url.toString}: ${e.getMessage}")
        Seq.empty
    }
  }

  def apply(jsonConfig: String): Seq[DeprecatedDependency] = Json.parse(jsonConfig).as[Seq[DeprecatedDependency]]
}
