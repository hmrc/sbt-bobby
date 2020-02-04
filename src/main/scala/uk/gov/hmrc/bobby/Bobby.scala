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

package uk.gov.hmrc.bobby

import java.net.URL

import sbt._
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.{Output, ViewType}

class BobbyValidationFailedException(message: String) extends RuntimeException(message)

object Bobby {

  private val logger         = ConsoleLogger()
  private val currentVersion = getClass.getPackage.getImplementationVersion

  def validateDependencies(
     projectDependencyMap: Map[ModuleID, Seq[ModuleID]],
     projectDependencies: Seq[ModuleID],
     pluginDependencies: Seq[ModuleID],
     scalaVersion: String,
     viewType: ViewType,
     bobbyRulesUrl: Option[URL] = None,
     jsonOutputFileOverride: Option[String] = None): Unit = {

    logger.info(s"[bobby] Bobby version $currentVersion")

    val config = new Configuration(bobbyRulesUrl, jsonOutputFileOverride)

    val messages =
      BobbyValidator.applyBobbyRules(projectDependencyMap, projectDependencies, pluginDependencies, config.loadBobbyRules)

    Output.writeMessages(messages, config.jsonOutputFile, config.textOutputFile, viewType)

    if(messages.exists(_.isError))
      throw new BobbyValidationFailedException("Build failed due to bobby violations. See previous output to resolve")
  }

  private[bobby] def filterDependencies(dependencies: Seq[ModuleID], ignoreList: Set[String]): Seq[ModuleID] =
    compactDependencies(dependencies)
      .filterNot(m => ignoreList.contains(m.organization))

  private[bobby] def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    import Util._
    dependencies
      .groupBy(_.moduleName)
      .map(_._2.head)
      .toSeq
  }

}
