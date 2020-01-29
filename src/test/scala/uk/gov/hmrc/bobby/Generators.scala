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

import net.virtualvoid.sbt.graph.{Edge, Module, ModuleGraph, ModuleId}
import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaChar, listOfN, _}

import scala.util.Random

object Generators {

  val r = new Random()

  val strGen: Int => Gen[String] = (n: Int) => listOfN(n, alphaChar).map(_.mkString)

  val nonEmptyString: Gen[String] = chooseNum(2, 3).flatMap(n => strGen(n))

  val organisationGen: Gen[String] = for {
    letter <- alphaChar
    number <- chooseNum(1,10)
  } yield s"uk.gov.$letter$number"

  val versionGen: Gen[String] = for {
    major <- chooseNum(0, 10)
    minor <- chooseNum(0, 100)
    patch <- chooseNum(0, 100)
    suffix <- oneOf("-SNAPSHOT", "-RC1", "")
  } yield s"$major.$minor.$patch$suffix"

  val artifactNameGen: Gen[String] = for {
    name <- nonEmptyString
    scalaVersion <- oneOf("_2.10", "_2.11", "_2.12", "")
  } yield s"$name$scalaVersion"

  def moduleIdGen(_nameGen: Gen[String] = artifactNameGen): Gen[ModuleId] =
    for {
      organisation <- organisationGen
      name <- _nameGen
      version <- versionGen
    } yield ModuleId(organisation, name, version)

  def moduleGen(_moduleIdGen: Gen[ModuleId] = moduleIdGen(),
               _evictedByVersionGen: Gen[Option[String]] = option(versionGen)): Gen[Module] =
    for {
      moduleId <- _moduleIdGen
      evictedByVersion <- _evictedByVersionGen
    } yield Module(moduleId, evictedByVersion = evictedByVersion)

  def edgeGen(nodes: Seq[ModuleId]): Gen[Seq[Edge]] = {
    if (nodes.size < 2)
      // Not enough nodes to make an edge
      Gen.const(Seq.empty)
    else {
      val numEdges = if (nodes.size < 2) 0 else r.nextInt(nodes.size)
      val edges = (0 until numEdges).map { fromNode =>
        // Always pick a node that is further down the chain from where we are now
        val toNode = fromNode + 1 + r.nextInt(numEdges - fromNode)

        Edge(nodes(fromNode), nodes(toNode))
      }
      Gen.const(edges)
    }
  }

  def moduleGraphGen(): Gen[ModuleGraph] =
    for {
      num <- chooseNum(1, 100)
      nodes <- listOfN(num, moduleGen())
      edges <- edgeGen(nodes.map(_.id))
    } yield ModuleGraph(nodes, edges)
}
