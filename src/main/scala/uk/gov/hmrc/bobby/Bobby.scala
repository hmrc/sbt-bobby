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
package uk.gov.hmrc.bobby

import java.util.concurrent.atomic.AtomicBoolean

import sbt.{ConsoleLogger, ModuleID, State}
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Bobby extends Bobby {
  override val checker: DependencyChecker = DependencyChecker
  override val nexus: Option[Nexus] = Nexus(Configuration.credentials)
}

trait Bobby {

  val logger = ConsoleLogger()

  val checker: DependencyChecker
  val nexus: Option[Nexus]

  def validateDependencies(dependencies: Seq[ModuleID], scalaVersion: String)(state: State): State = {
    if(areDependenciesValid(dependencies, scalaVersion)) state else state.exit(true)
  }

  def areDependenciesValid(dependencies: Seq[ModuleID], scalaVersion: String): Boolean = {

    logger.info(s"[bobby] Checking dependencies")
    val compacted = compactDependencies(dependencies)

    val result = new AtomicBoolean(true)

    compacted.par.foreach(module => {

      val latestRevision = nexus.flatMap { n =>
        n.findLatestRevision(module, scalaVersion)
      }

      DependencyChecker.isDependencyValid(Dependency(module.organization, module.name), Version(module.revision)) match {
        case MandatoryFail(latest) =>
          logger.error(s"[bobby] '${module.name} ${module.revision}' is deprecated and has to be upgraded! " +
            s"Reason: ${latest.reason}. " +
            s"${latestRevision.map(v => s"Please consider using '$v' instead").getOrElse("")}")
          result.set(false)
        case MandatoryWarn(latest) =>
          logger.warn(s"[bobby] '${module.name} ${module.revision}' is deprecated! " +
            s"You will not be able to use it after ${latest.from}.  " +
            s"Reason: ${latest.reason}. Please consider upgrading" +
            s"${latestRevision.map(v => s"to '$v'").getOrElse("")}")
        case _ =>
          if (nexus.isDefined && latestRevision.isEmpty)
            logger.info(s"[bobby] Unable to get a latestRelease number for '${module.toString()}'")
          else if (latestRevision.isDefined && Version(latestRevision.get).isAfter(Version(module.revision)))
            logger.info(s"[bobby] '${module.name} ${module.revision}' is out of date, consider upgrading to '${latestRevision.get}'")

      }
    })
    result.get
  }


  def compactDependencies(dependencies: Seq[ModuleID]) = {

    val b = new ListBuffer[ModuleID]()
    val seen = mutable.HashSet[String]()
    for (x <- dependencies) {
      if (!seen(s"${x.organization}.${x.name}")) {
        b += x
        seen += s"${x.organization}.${x.name}"
      }
    }
    b.toSeq
  }
}
