/*
 * Copyright 2016 HM Revenue & Customs
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

import sbt._


//
// heavily influenced by https://github.com/jozic/sbt-about-plugins/blob/master/src/main/scala/com/github/sbt/aboutplugins/AboutPluginsPlugin.scala.
//
// - minor changes to get all the plugins by merging auto plugins
//

object ProjectPlugin {

  def plugins(build: BuildStructure): Seq[ModuleID] = {

    val pluginNamesAndLoaders = build.units.values.map {
      u =>
        val pNames = u.unit.plugins.detected.autoPlugins.map(_.name).toList ::: u.unit.plugins.detected.plugins.names.toList
        (pNames, u.unit.plugins.loader)
    }.toSeq

    val pluginArtifactPaths: Seq[(String, String)] = for {
      (names, loader) <- pluginNamesAndLoaders
      name <- names
      source <- Option(Class.forName(name, true, loader).getProtectionDomain.getCodeSource)
      location <- Option(source.getLocation)
    } yield (name, location.getPath)

    val reports: Seq[UpdateReport] = build.units.values.flatMap(un => un.unit.plugins.pluginData.report).toSeq

    reports.flatMap {
      report =>
        val moduleReports: Map[ModuleID, Seq[(Artifact, File)]] = (for {
          configReport <- report.configurations
          moduleReport <- configReport.modules
        } yield moduleReport.module -> moduleReport.artifacts).toMap

        for {
          (name, artifactPath) <- pluginArtifactPaths
          (module, artifacts) <- moduleReports
          if artifacts.exists {
            case (_, file) =>
              artifactPath == file.getPath
          }
        } yield module
    }

  }


}
