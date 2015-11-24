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

package uk.gov.hmrc.bobby.domain

import sbt.ModuleID
import scala.util.Try

object MessageBuilder{

  def makeMessage(pLevel: MessageLevels.Level, pMessage: String) = new Message {
    override val level = pLevel
    override def jsonMessage: String = pMessage

    override def module: ModuleID = ???

    override def latestRevision: Try[Version] = ???

    override def tabularMessage: String = ???
  }
}
