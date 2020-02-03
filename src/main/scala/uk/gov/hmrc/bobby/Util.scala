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

import fansi.Str
import net.virtualvoid.sbt.graph.ModuleId
import sbt.librarymanagement.ModuleID
import uk.gov.hmrc.bobby.domain.Dependency

object Util {

  implicit class ExtendedModuleId(id: ModuleId) {
    def toSbt = ModuleID(id.organisation, id.name, id.version)
    def toDependency() = Dependency(id.organisation, id.name)
  }

  implicit class ExtendedSbtModuleID(id: ModuleID) {
    def toDependencyGraph = ModuleId(id.organization, id.name, id.revision)
    def toDependency() = Dependency(id.organization, id.name)
    def moduleName = s"${id.organization}.${id.name}"
  }

  implicit class FansiStr(s: String) {
    def fansi = Str(s)
  }

}
