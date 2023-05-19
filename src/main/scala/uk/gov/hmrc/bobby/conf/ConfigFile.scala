/*
 * Copyright 2023 HM Revenue & Customs
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

import java.io.File

import sbt.util.Logger

import scala.io.Source

trait ConfigFile {
  def get(path: String): Option[String]
  def fileName: String
}

case class ConfigFileImpl(fileName: String, logger: Logger) extends ConfigFile {
  if (!new File(fileName).exists()) {
    logger.warn(s"Supplied configuration file '$fileName' does not exist.")
  }

  private def loadKvMap: Map[String, String] =
    try {
      val source = Source.fromFile(fileName)
      val lines = source.getLines().toList
      source.close()
      BobbyConfiguration.extractMap(lines)
    } catch {
      case e: Exception =>
        logger.debug(s"[bobby] Unable to find $fileName. ${e.getClass.getName}: ${e.getMessage}")
        Map.empty
    }

  val kvMap: Map[String, String] = loadKvMap

  def get(path: String): Option[String] =
    kvMap.get(path)
}
