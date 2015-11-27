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

package uk.gov.hmrc.bobby.conf

import java.net.URL

import play.api.libs.json.Json

//import play.api.libs.json.Json
import scala.util.parsing.json._
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain.DeprecatedDependency

import scala.io.Source

object Configuration {

  val timeout = 3000
  val logger = ConsoleLogger()

  val bobbyConfigFile = System.getProperty("user.home") + "/.sbt/bobby.conf"

  val deprecatedDependencies: Seq[DeprecatedDependency] = {

    val bobbyConfig: Option[String] = new ConfigFile(bobbyConfigFile).get("deprecated-dependencies")

    bobbyConfig.fold {
      logger.warn(s"[bobby] Unable to check for explicitly deprecated dependencies - $bobbyConfigFile does not exist or is not configured with deprecated-dependencies or may have trailing whitespace")
      Seq.empty[DeprecatedDependency]
    } { c =>
      try {
        val conn = new URL(c).openConnection()
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)
        val inputStream = conn.getInputStream

        this(Source.fromInputStream(inputStream).mkString)
      } catch {
        case e: Exception =>
          logger.warn(s"[bobby] Unable load configuration from $c: ${e.getMessage}")
          Seq.empty
      }
    }
  }

  val jsonOutputFile: String = new ConfigFile(bobbyConfigFile).get("output-file").getOrElse("./target/bobby-reports/bobby-report.json")
  val textOutputFile: String = new ConfigFile(bobbyConfigFile).get("text-output-file").getOrElse("./target/bobby-reports/bobby-report.txt")

  val credsFile = System.getProperty("user.home") + "/.sbt/.credentials"

  val nexusCredetials: Option[NexusCredentials] = {
    val ncf = new ConfigFile(credsFile)

    for {
      host <- ncf.get("host")
      user <- ncf.get("user")
      password <- ncf.get("password")

    } yield NexusCredentials(host, user, password)
  }

  val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

  val bintrayCredetials: Option[BintrayCredentials] = {
    val bncf = new ConfigFile(bintrayCredsFile)

    for {
      user <- bncf.get("user")
      password <- bncf.get("password")

    } yield BintrayCredentials(user, password)
  }

  def apply(jsonConfig: String): Seq[DeprecatedDependency] = Json.parse(jsonConfig).as[Seq[DeprecatedDependency]]
}

