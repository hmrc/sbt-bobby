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

package uk.gov.hmrc.bobby.conf

import java.net.URL

import org.joda.time.LocalDate
import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.domain._

import scala.io.Source
import scala.util.parsing.json.JSON

object Configuration {

  val credsFile        = System.getProperty("user.home") + "/.sbt/.credentials"
  val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

  val defaultJsonOutputFile = "./target/bobby-reports/bobby-report.json"
  val defaultTextOutputFile = "./target/bobby-reports/bobby-report.txt"

  def parseConfig(jsonConfig: String): List[DeprecatedDependency] = {
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
        } yield DeprecatedDependency.apply(Dependency(organisation, name), VersionRange(range), reason, fromDate, typ)
      }).toList.flatten
    }).getOrElse(List.empty)

  }

  val nexusCredetials: Option[NexusCredentials] = {
    val ncf = new ConfigFile(credsFile)

    for {
      host     <- ncf.get("host")
      user     <- ncf.get("user")
      password <- ncf.get("password")

    } yield NexusCredentials(host, user, password)
  }

  val bintrayCredetials: Option[BintrayCredentials] = {
    val bncf = new ConfigFile(bintrayCredsFile)

    for {
      user     <- bncf.get("user")
      password <- bncf.get("password")

    } yield BintrayCredentials(user, password)
  }

  val artifactoryUri: Option[String] = sys.env.get("ARTIFACTORY_URI")
}

class Configuration(
  url: Option[URL] = None,
  jsonOutputFileOverride: Option[String]
) {

  import Configuration._

  val timeout = 3000
  val logger  = ConsoleLogger()

  val bobbyConfigFile = System.getProperty("user.home") + "/.sbt/bobby.conf"

  val jsonOutputFile: String =
    (jsonOutputFileOverride orElse new ConfigFile(bobbyConfigFile).get("output-file")).getOrElse(defaultJsonOutputFile)
  val textOutputFile: String = new ConfigFile(bobbyConfigFile).get("text-output-file").getOrElse(defaultTextOutputFile)

  def loadDeprecatedDependencies: DeprecatedDependencies = {

    val bobbyConfig: Option[URL] = url orElse new ConfigFile(bobbyConfigFile).get("deprecated-dependencies").map { u =>
      new URL(u)
    }

    bobbyConfig.fold {
      logger.warn(
        s"[bobby] Unable to check for explicitly deprecated dependencies - $bobbyConfigFile does not exist or is not configured with deprecated-dependencies or may have trailing whitespace")
      DeprecatedDependencies.EMPTY
    } { c =>
      try {
        logger.info(s"[bobby] loading deprecated dependency list from $c")
        val conn = c.openConnection()
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)
        val inputStream = conn.getInputStream

        DeprecatedDependencies(Configuration.parseConfig(Source.fromInputStream(inputStream).mkString))

      } catch {
        case e: Exception =>
          logger.warn(s"[bobby] Unable load configuration from $c: ${e.getMessage}")
          DeprecatedDependencies.EMPTY
      }
    }
  }
}
