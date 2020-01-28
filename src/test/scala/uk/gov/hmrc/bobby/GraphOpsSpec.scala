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

import net.virtualvoid.sbt.graph.{Module, ModuleGraph, ModuleId}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.bobby.Generators._

class GraphOpsSpec extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  "stripScalaVersionSuffix" should "remove the scala suffix" in {
    forAll(moduleGen(_moduleIdGen = moduleIdGen(_nameGen = Gen.oneOf("auth_2.10", "auth_2.11", "auth_2.12")))) { module =>
      val stripped = GraphOps.stripScalaVersionSuffix(ModuleGraph(Seq(module), Seq.empty))
      stripped.nodes.head.id.name shouldBe "auth"
    }
  }

  it should "not modify something without the suffix" in {
    forAll(moduleGen(_moduleIdGen = moduleIdGen(_nameGen = Gen.oneOf("auth", "auth_2", "auth-lib", "auth_2.1")))) { module =>
      val stripped = GraphOps.stripScalaVersionSuffix(ModuleGraph(Seq(module), Seq.empty))
      stripped.nodes.head.id.name shouldBe module.id.name
    }
  }

  "transpose" should "reverse the edges of the graph" in {
    forAll(moduleGraphGen()) { moduleGraph =>
      val transposedEdges = GraphOps.transpose(moduleGraph)

      transposedEdges.edges.map(_.swap) shouldBe moduleGraph.edges
    }
  }

  "pruneEvicted" should "removes any edges that point to evicted nodes" in {
    forAll(moduleGraphGen()) { moduleGraph =>
      val prunedGraph = GraphOps.pruneEvicted(moduleGraph)

      val evictedNodes = moduleGraph.nodes.filter(_.isEvicted).map(_.id)

      val usedNodes = prunedGraph.edges.flatMap(e => Seq(e._1,  e._2))
      usedNodes.toSet.intersect(evictedNodes.toSet) shouldBe Set.empty
    }
  }

  "topoSort" should "do a basic sort a->b" in {
    val a = ModuleId("A", "a", "v1")
    val b = ModuleId("B", "b", "v1")
    val graph = ModuleGraph(nodes = Seq(Module(a), Module(b)),
      edges = Seq((a,b))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(a,b)
  }

  it should "do a basic sort b->a" in {
    val a = ModuleId("A", "a", "v1")
    val b = ModuleId("B", "b", "v1")
    val graph = ModuleGraph(nodes = Seq(Module(a), Module(b)),
      edges = Seq((b,a))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(b,a)
  }

  it should "do a basic sort a->b->c" in {
    val a = ModuleId("A", "a", "v1")
    val b = ModuleId("B", "b", "v1")
    val c = ModuleId("C", "c", "v1")
    val graph = ModuleGraph(nodes = Seq(Module(a), Module(b), Module(c)),
      edges = Seq((a,b), (b,c))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(a, b, c)
  }

  it should "do a basic sort a->c->b" in {
    val a = ModuleId("A", "a", "v1")
    val b = ModuleId("B", "b", "v1")
    val c = ModuleId("C", "c", "v1")
    val graph = ModuleGraph(nodes = Seq(Module(a), Module(b), Module(c)),
      edges = Seq((a,c), (c,b))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(a, c, b)
  }

  it should "do a complex sort a->b, c->e->d (->a), g->h, j->k->i->f" in {
    val a = ModuleId("A", "a", "v1")
    val b = ModuleId("B", "b", "v1")
    val c = ModuleId("C", "c", "v1")
    val d = ModuleId("D", "d", "v1")
    val e = ModuleId("E", "e", "v1")
    val f = ModuleId("F", "f", "v1")
    val g = ModuleId("G", "g", "v1")
    val h = ModuleId("H", "h", "v1")
    val i = ModuleId("I", "i", "v1")
    val j = ModuleId("J", "j", "v1")
    val k = ModuleId("K", "k", "v1")
    val graph = ModuleGraph(nodes = Seq(Module(a), Module(b), Module(c), Module(d), Module(e), Module(f), Module(g), Module(h), Module(i), Module(j),Module(k)),
      edges = Seq((a,b), (c,e), (e,d), (g,h), (j,k), (k,i), (i,f), (d,a))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(c,g,j,e,h,k,d,i,a,f,b)
  }

  it should "always have the roots first" in {
    forAll(moduleGraphGen()) { moduleGraph =>
      val topoSorted = GraphOps.topoSort(moduleGraph)
      val roots = moduleGraph.roots.sortBy(_.id.name)

      topoSorted.take(roots.length) shouldBe roots.map(_.id)
    }
  }

}
