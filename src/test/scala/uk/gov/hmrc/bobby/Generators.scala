/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaChar, chooseNum, listOfN, oneOf}
import uk.gov.hmrc.bobby.domain.Dependency

object Generators {

  val strGen: Int => Gen[String] =
    (n: Int) => listOfN(n, alphaChar).map(_.mkString)

  val nonEmptyString: Gen[String] =
    chooseNum(2, 3).flatMap(n => strGen(n))

  val organisationGen: Gen[String] =
    for {
      letter <- nonEmptyString
      number <- chooseNum(1,10)
    } yield s"uk.gov.$letter$number"

  val versionGen: Gen[String] =
    for {
      major  <- chooseNum(0, 10)
      minor  <- chooseNum(0, 100)
      patch  <- chooseNum(0, 100)
      suffix <- oneOf("-SNAPSHOT", "-RC1", "")
    } yield s"$major.$minor.$patch$suffix"

  val artifactNameGen: Gen[String] =
    for {
      name         <- nonEmptyString
      scalaVersion <- oneOf("_2.10", "_2.11", "_2.12", "")
    } yield s"$name$scalaVersion"

  val depedendencyGen: Gen[Dependency] =
    for {
      org  <- organisationGen
      name <- nonEmptyString
    } yield Dependency(org, name)
}
