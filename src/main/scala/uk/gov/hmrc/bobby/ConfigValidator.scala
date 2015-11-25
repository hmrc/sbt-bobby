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

package uk.gov.hmrc.bobby

import java.io.File

import play.api.libs.json.Json
import uk.gov.hmrc.bobby.domain.DeprecatedDependency

import scala.io.Source

object ConfigValidator {
  def main(args: Array[String]) {

    if(args.length != 1){
      throw new IllegalArgumentException("pass one file to the validator")
    } else {
      val file = args.head
      Json.parse(Source.fromFile(new File(file)).mkString).as[Seq[DeprecatedDependency]]
    }
  }
}
