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

package uk.gov.hmrc.bobby.domain

object MessageLevels {

  sealed abstract class Level(
                                val order: Int,
                                val name: String) extends Ordered[Level] {

    def compare(that: Level) = this.order - that.order

    override def toString = name
  }

  def compare(a:Level, b:Level):Boolean = a.compare(b) < 0

  case object ERROR extends Level(0, "ERROR")
  case object WARN extends Level(1, "WARN")
  case object INFO extends Level(2, "INFO")

}
