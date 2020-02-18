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

import net.virtualvoid.sbt.graph.{GraphTransformations, Module, ModuleGraph, ModuleId}
import sbt.ModuleID
import uk.gov.hmrc.bobby.Util._

import scala.annotation.tailrec

// Initially inspired by https://github.com/Verizon/sbt-blockade/blob/1f64972703f73267bf9f8607d736516f013ac07b/src/main/scala/verizon/build/blockade.scala#L375-L433
object GraphOps {

  /**
   * Make the arrows go in the opposite direction (reverse the direction of all edges)
   */
  def transpose(g: ModuleGraph): ModuleGraph =
    g.copy(edges = g.edges.map { case (from, to) => (to, from) })

  /**
   * Topological sort a ModuleGraph, returning just the sequence of ModuleId's
   */
  def topoSort(g: ModuleGraph): Seq[ModuleId] = {
    def removeNodes(g: ModuleGraph, nodesForRemovalIds: Seq[ModuleId]): ModuleGraph = {
      val updatedNodes = g.nodes.filter(n => !nodesForRemovalIds.contains(n.id))
      val updatedEdges = g.edges.filter(e => !nodesForRemovalIds.contains(e._1))

      ModuleGraph(updatedNodes, updatedEdges)
    }

    // Recursively builds a list of ModuleIDs in sequence starting from the nodes, chopping off the nodes
    // and making a new graph, then repeating.
    @tailrec
    def go(curGraph: ModuleGraph, acc: Seq[ModuleId]): Seq[ModuleId] = {
      if (curGraph.nodes.isEmpty) acc
      else {
        val roots = curGraph.roots.map(_.id).sortBy(_.name)
        go(removeNodes(curGraph, roots), acc ++ roots)
      }
    }

    go(g, Seq.empty)
  }

  /**
   * Remove any edges that point to evicted dependencies.
   *
   * Ivy (and sbt-dependency-graph) gives us a DAG containing
   * evicted modules, but not the evicted modules' dependencies
   * (unless those sub-dependencies are used by a non-evicted module,
   * in which case we want to keep them anyway).
   * So, we need only remove evicted nodes and their in-bound (and out-bound,
   * if they happen to exist) edges.
   */
  def pruneEvicted(g: ModuleGraph): ModuleGraph = pruneNodes(g, m => m.isEvicted)

  def pruneNodes(g: ModuleGraph, pruneFn: Module => Boolean): ModuleGraph = {
    val usedModules = g.nodes.filterNot(pruneFn)
    val usedModuleIds = usedModules.map(_.id).toSet
    val legitEdges = g.edges.filter {
      case (from, to) =>
        usedModuleIds.contains(from) && usedModuleIds.contains(to)
    }

    g.copy(
      nodes = usedModules,
      edges = legitEdges
    )

  }

  def stripUnderscore(mid: ModuleId): ModuleId = {
    val updatedName = mid.name.split("_2\\.\\d{2}").head
    mid.copy(name = updatedName)
  }

  // The full module graph includes the full artifact with names like:
  // uk.gov.hmrc.simple-reactivemongo_2.11
  // We want to strip off the _2.11 suffix so that the output from transitive dependencies matches what is shown
  // for local libraryDependencies
  def stripScalaVersionSuffix(g: ModuleGraph): ModuleGraph = {
    g.copy(
      nodes = g.nodes.map(n => n.copy(id = stripUnderscore(n.id))),
      edges = g.edges.map {
        case (from, to) => stripUnderscore(from) -> stripUnderscore(to)
      }
    )
  }

  // Build a dependency map which is keyed by the leaves, and contains a linear sequence of nodes that lead to the root that
  // brought in that particular leaf dependency
  // The sbt-dependency-graph plugin can build a reverse dependency map but it only considers one level
  // i.e. if we have a->b->c :
  // With sbt-dependency-graph we'll get a map: C->B, B->A. i.e. C does not contain the line all the way back to A
  // With this method, we return a richer map: C->B,A B->A  i.e. each node has all of the connections to get right back to the root
  def reverseDependencyMap(graph: ModuleGraph, moduleIDs: Seq[ModuleId]): Map[ModuleId, Seq[ModuleId]] = {
    moduleIDs.map { id =>
      val localGraph = GraphTransformations.reverseGraphStartingAt(graph, id)
      val topoSorted = GraphOps.topoSort(localGraph)
      id -> topoSorted.filterNot(i => i == id) //Filter out the nodes themselves from the ancestry line, i.e. instead of C->C,B,A leave just C->B,A
    }.toMap
  }

  def toSbtDependencyMap(map: Map[ModuleId, Seq[ModuleId]]): Map[ModuleID, Seq[ModuleID]] =
    map.map { case (k, v) => k.toSbt -> v.map(_.toSbt)}

  def cleanGraph(graph: ModuleGraph, excludeNodes: Seq[ModuleId]): ModuleGraph = {
    val stripped = excludeNodes.map(stripUnderscore)

    // Remove evicted nodes and the current project node
    val pruned = pruneNodes(
      pruneEvicted(graph),
      m => stripped.contains(stripUnderscore(m.id))
    )
    // Remove the '_2.11' suffixes etc from the artefact names
    stripScalaVersionSuffix(pruned)
  }

}
