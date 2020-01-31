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

  object deps {
    val A = ModuleId("A", "a", "v1")
    val B = ModuleId("B", "b", "v1")
    val C = ModuleId("C", "c", "v1")
    val D = ModuleId("D", "d", "v1")
    val E = ModuleId("E", "e", "v1")
    val F = ModuleId("F", "f", "v1")
    val G = ModuleId("G", "g", "v1")
    val H = ModuleId("H", "h", "v1")
    val I = ModuleId("I", "i", "v1")
    val J = ModuleId("J", "j", "v1")
    val K = ModuleId("K", "k", "v1")
  }
  import deps._

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

  "pruneNodes" should "remove a particular node c in a->b->c" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B), Module(C)),
      edges = Seq((A,B), (B,C))
    )
    val pruned = GraphOps.pruneNodes(graph, m => m.id == C)
    pruned.nodes.map(_.id) shouldBe Seq(A, B)
  }

  it should "remove any arbitrary node" in {
    forAll(moduleGraphGen()) { graph =>
      val toPrune = graph.nodes.head
      val pruned = GraphOps.pruneNodes(graph, m => m.id == toPrune.id)
      pruned.nodes.map(_.id).contains(toPrune.id) shouldBe false
      pruned.nodes.size shouldBe graph.nodes.size - 1
    }
  }

  "topoSort" should "do a basic sort a->b" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B)),
      edges = Seq((A,B))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(A,B)
  }

  it should "do a basic sort b->a" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B)),
      edges = Seq((B,A))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(B,A)
  }

  it should "do a basic sort a->b->c" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B), Module(C)),
      edges = Seq((A,B), (B,C))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(A, B, C)
  }

  it should "do a basic sort a->c->b" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B), Module(C)),
      edges = Seq((A,C), (C,B))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(A, C, B)
  }

  it should "do a complex sort a->b, c->e->d (->a), g->h, j->k->i->f" in {

    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B), Module(C), Module(D), Module(E), Module(F), Module(G), Module(H), Module(I), Module(J),Module(K)),
      edges = Seq((A,B), (C,E), (E,D), (G,H), (J,K), (K,I), (I,F), (D,A))
    )
    val topoSorted = GraphOps.topoSort(graph)
    topoSorted shouldBe Seq(C,G,J,E,H,K,D,I,A,F,B)
  }

  it should "always have the roots first" in {
    forAll(moduleGraphGen()) { moduleGraph =>
      val topoSorted = GraphOps.topoSort(moduleGraph)
      val roots = moduleGraph.roots.sortBy(_.id.name)

      topoSorted.take(roots.length) shouldBe roots.map(_.id)
    }
  }

  "dependencyMap" should "map basic a->b" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B)),
      edges = Seq((A,B))
    )
    val map = GraphOps.reverseDependencyMap(graph, Seq(A,B))
    map shouldBe Map(B -> Seq(A), A -> Seq.empty)
  }

  it should "map with indirect edges a->b->c" in {
    val graph = ModuleGraph(nodes = Seq(Module(A), Module(B), Module(C)),
      edges = Seq((A,B), (B,C))
    )
    val map = GraphOps.reverseDependencyMap(graph, Seq(A,B,C))
    map shouldBe Map(C -> Seq(B, A), B -> Seq(A), A -> Seq.empty)
  }

  it should "map with a complex graph" in {
    forAll(moduleGraphGen()) { graph =>

      val pruned = GraphOps.pruneEvicted(graph)

      // Get the nodes of the graph
      val nodes = pruned.nodes.map(_.id)

      // Build the reverse dependency map, via sbt-dependency-graph
      val sbtDepGraphReverseMap = pruned.reverseDependencyMap

      // Build the richer reverse dependency graph
      val map = GraphOps.reverseDependencyMap(pruned, nodes)

      // For every node in the graph, the list of connected nodes should match with the head from sbt-dependency-graph
      nodes.map { id =>
        val fullPathBackToRoot = map.getOrElse(id, Seq.empty)
        val partialPathBackToRoot = sbtDepGraphReverseMap.getOrElse(id, Seq.empty).map(_.id)

        fullPathBackToRoot.headOption shouldBe partialPathBackToRoot.headOption
      }
    }
  }

}
