/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{Compact, ViewType}

import scala.io.Source

object BobbyConfiguration {

  val defaultOutputDirectory = "./target/bobby-reports"

  def parseConfig(jsonConfig: String): List[BobbyRule] = {
    val reads = {
      val readsBobbyRules =
        Reads.list(BobbyRule.reads)

      ( (__ \ "libraries").read(readsBobbyRules)
      ~ (__ \ "plugins"  ).read(readsBobbyRules)
      )(_ ++ _)
    }

    Json.fromJson(Json.parse(jsonConfig))(reads).getOrElse(List.empty)
  }

  def extractMap(lines: List[String]): Map[String, String] =
    (for {
       line              <- lines
       Array(key, value) =  line.split("=", 2)
     } yield key.trim -> value.trim
    ).toMap
}

case class BobbyConfiguration(
  bobbyRulesURL          : Option[URL]        = None,
  outputDirectoryOverride: Option[String]     = None,
  outputFileName         : String             = "bobby-report",
  bobbyConfigFile        : Option[ConfigFile] = None,
  strictMode             : Boolean            = false,
  viewType               : ViewType           = Compact,
  consoleColours         : Boolean            = true
) {
  import BobbyConfiguration._

  val timeout = 3000
  val logger  = ConsoleLogger()

  def configValue(key: String): Option[String] =
    bobbyConfigFile.flatMap(_.get(key))

  val outputDirectory: String =
    outputDirectoryOverride
      .orElse(configValue("output-directory"))
      .getOrElse(defaultOutputDirectory)

  val resolvedRuleUrl: Option[URL] =
    bobbyRulesURL.map { url =>
      logger.info(s"[bobby] Bobby rule location was set explicitly in build")
      url
    }.orElse {
      logger.info(s"[bobby] Looking for bobby rule location in config file: ${bobbyConfigFile.map(_.fileName).getOrElse("Not set")}")
      configValue("bobby-rules-url").map(new URL(_))
    }

  def loadBobbyRules(): List[BobbyRule] =
    resolvedRuleUrl.map { url =>
      try {
        logger.info(s"[bobby] Loading bobby rules from: $url")
        val conn = url.openConnection()
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)
        val inputStream = conn.getInputStream
        val rules = BobbyConfiguration.parseConfig(Source.fromInputStream(inputStream).mkString)
        logger.info(s"[bobby] Found ${rules.size} rules")
        rules
      } catch {
        case e: Exception => abort(s"Unable to load bobby rules from $url: ${e.getMessage}")
      }
    }.getOrElse(abort("Bobby rule location unknown! - Set 'bobbyRulesURL' via the config file or explicitly in the build"))

  def abort(message: String): Nothing = {
    logger.error(s"[bobby] $message")
    sys.error(message)
  }
}
