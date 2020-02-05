/*
 * Copyright 2020 HM Revenue & Customs
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
import java.time.LocalDate

import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain._

import scala.io.Source
import scala.util.parsing.json.JSON

object Configuration {

  val defaultJsonOutputFile = "./target/bobby-reports/bobby-report.json"
  val defaultTextOutputFile = "./target/bobby-reports/bobby-report.txt"

  def parseConfig(jsonConfig: String): List[BobbyRule] = {
    import uk.gov.hmrc.bobby.NativeJsonHelpers._

    (for (M(map) <- JSON.parseFull(jsonConfig)) yield {
      (for {
        (S(key), L(list)) <- map
        typ = DependencyType(key)
        if typ != Unknown
      } yield {
        for {
          MS(mapS)     <- list
          organisation <- mapS.get("organisation")
          name         <- mapS.get("name")
          range        <- mapS.get("range")
          reason       <- mapS.get("reason")
          fromString   <- mapS.get("from")
          fromDate = LocalDate.parse(fromString)
        } yield BobbyRule.apply(Dependency(organisation, name), VersionRange(range), reason, fromDate, typ)
      }).toList.flatten
    }).getOrElse(List.empty)

  }

  def extractMap(lines: List[String]): Map[String, String] = {
    (for {
      line <- lines
      Array(key, value) = line.split("=", 2)
    } yield key.trim -> value.trim).toMap
  }

}

class Configuration(
  bobbyRuleURL: Option[URL] = None,
  bobbyConfigFile: Option[ConfigFile] = None,
  jsonOutputFileOverride: Option[String] = None
) {

  import Configuration._

  val timeout = 3000
  val logger  = ConsoleLogger()

  def configValue(key: String): Option[String] = bobbyConfigFile.flatMap(_.get(key))

  val jsonOutputFile: String = (jsonOutputFileOverride orElse configValue("json-output-file")).getOrElse(defaultJsonOutputFile)
  val textOutputFile: String = configValue("text-output-file").getOrElse(defaultTextOutputFile)

  def loadBobbyRules(): BobbyRules = {

    val resolvedRuleUrl: Option[URL] = bobbyRuleURL.map { url =>
      logger.info(s"[bobby] Bobby rule location was set explicitly in build")
      url
    } orElse {
      logger.info(s"[bobby] Looking for bobby rule location in config file: ${bobbyConfigFile}")
      configValue("deprecated-dependencies").map(new URL(_))
    }

    resolvedRuleUrl.map { url =>
      try {
        logger.info(s"[bobby] Loading bobby rules from: $url")
        val conn = url.openConnection()
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)
        val inputStream = conn.getInputStream
        BobbyRules(Configuration.parseConfig(Source.fromInputStream(inputStream).mkString))
      } catch {
        case e: Exception => abort(s"Unable to load bobby rules from $url: ${e.getMessage}")
      }
    }.getOrElse(abort("Bobby rule location unknown! - Set 'deprecatedDependenciesUrl' via the config file or explicitly in the build"))
  }

  def abort(message: String): Nothing = {
    logger.error(s"[bobby] $message")
    sys.error(message)
  }

}
