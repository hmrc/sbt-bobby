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

import net.virtualvoid.sbt.graph.{ModuleGraph, ModuleId}

// Modified from https://github.com/Verizon/sbt-blockade/blob/1f64972703f73267bf9f8607d736516f013ac07b/src/main/scala/verizon/build/blockade.scala#L375-L433
object GraphOps {
  /**
   * Make the arrows go in the opposite direction.
   *
   * @param g
   * @return
   */
  def transpose(g: ModuleGraph): ModuleGraph =
    g.copy(edges = g.edges.map { case (from, to) => (to, from) })

  /**
   * Topological sort a ModuleGraph.
   *
   * @param g
   * @return
   */
  def topoSort(g: ModuleGraph): Seq[ModuleId] = {
    def removeNodes(g: ModuleGraph, nodesForRemovalIds: Seq[ModuleId]): ModuleGraph = {
      val updatedNodes = g.nodes.filter(n => !nodesForRemovalIds.contains(n.id))
      val updatedEdges = g.edges.filter(e => !nodesForRemovalIds.contains(e._1))

      ModuleGraph(updatedNodes, updatedEdges)
    }

    def go(curGraph: ModuleGraph, acc: Seq[ModuleId]): Seq[ModuleId] = {
      if (curGraph.nodes.isEmpty) acc
      else {
        val roots = curGraph.roots.map(_.id)
        go(removeNodes(curGraph, roots), acc ++ roots)
      }
    }

    go(g, Seq.empty)
  }

  /**
   * Ivy (and sbt-dependency-graph) gives us a DAG containing
   * evicted modules, but not the evicted modules' dependencies
   * (unless those sub-dependencies are used by a non-evicted module,
   * in which case we want to keep them anyway).
   * So, we need only remove evicted nodes and their in-bound (and out-bound,
   * if they happen to exist) edges.
   */
  def pruneEvicted(g: ModuleGraph): ModuleGraph = {
    val usedModules = g.nodes.filterNot(_.isEvicted)
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

  // The full module graph includes the full artifact with names like:
  // uk.gov.hmrc.simple-reactivemongo_2.11
  // We want to strip off the _2.11 suffix so that the output from transitive dependencies matches what is shown
  // for local libraryDependencies
  def stripScalaVersionSuffix(g: ModuleGraph): ModuleGraph = {
    def stripUnderscore(mid: ModuleId): ModuleId = {
      val updatedName = mid.name.split("_2\\.\\d{2}").head
      mid.copy(name = updatedName)
    }
    g.copy(
      nodes = g.nodes.map(n => n.copy(id = stripUnderscore(n.id))),
      edges = g.edges.map {
        case (from, to) => stripUnderscore(from) -> stripUnderscore(to)
      }
    )
  }

}
