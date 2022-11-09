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

package uk.gov.hmrc.graph

import scala.annotation.tailrec

// TODO lifted from service-dependencies (also used by pr-commenter etc.)
// should we create a library?
class DependencyGraphParser {
  import DependencyGraphParser._

  def parse(input: String): DependencyGraph = {
    val graph = lexer(input.split("\n").toIndexedSeq)
      .foldLeft(DependencyGraph.empty){ (graph, t)  =>
        t match {
          case n: Node     => graph.copy(nodes     = graph.nodes      + n)
          case a: Arrow    => graph.copy(arrows    = graph.arrows     + a)
          case e: Eviction => graph.copy(evictions = graph.evictions  + e)
        }
      }

    // maven generated digraphs don't add nodes, but the main dependency is named the same as the digraph
    if (graph.nodes.isEmpty) {
      val nodesFromRoot = graph.arrows.map(_.to) ++ graph.arrows.map(_.from)
      graph.copy(nodes = nodesFromRoot)
    } else {
      graph
    }
  }

  private val eviction = """\s*"(.+)" -> "(.+)" \[(.+)\]\s*""".r
  private val arrow    = """\s*"(.+)" -> "(.+)"\s*;?\s*""".r
  private val node     = """\s*"(.+)"\[(.+)\]\s*""".r

  private def lexer(lines: Seq[String]): Seq[Token] =
    lines.flatMap {
      case node(n, _)        => Some(Node(n))
      case arrow(a,b)        => Some(Arrow(Node(a), Node(b)))
      case eviction(a, b, r) => Some(Eviction(Node(a), Node(b), r))
      case _                 => None
    }
}

object DependencyGraphParser {
  private val group           = """([^:]+)"""
  private val artefact        = """([^:]+?)"""          // make + lazy so it does not capture the optional scala version
  private val optScalaVersion = """(?:_(\d+\.\d+))?"""  // non-capturing group to get _ and optional scala version
  private val optType         = """(?::(?:jar|war|test-jar|pom|zip|txt))?""" // non-capturing group to get optional type
  private val optClassifier   = """(?::[^:]+)??"""      // a lazy non-capturing group to get optional classifier, lazy so it doesn't eat the version
  private val version         = """([^:]+)"""
  private val optScope        = """(?::(?:compile|runtime|test|system|provided))?""" // optional non-capturing group for scope
  private val nodeRegex       = (s"$group:$artefact$optScalaVersion$optType$optClassifier:$version$optScope").r

  sealed trait Token

  case class Node(value: String) extends Token {

    private lazy val nodeRegex(g, a, sv, v) = value

    def group        = g
    def artefact     = a
    def version      = v
    // TODO review this (changed from service-dependencies)
    def artefactWithoutScalaVersion =
      artefact.split("_2\\.\\d{2}").head

    def scalaVersion = Option(sv)
  }

  case class Arrow(from: Node, to: Node) extends Token

  case class Eviction(old: Node, by: Node, reason: String) extends Token

  case class DependencyGraph(
    nodes    : Set[Node],
    arrows   : Set[Arrow],
    evictions: Set[Eviction]
  ) {
    def dependencies: Seq[Node] =
      nodes.toList
        .filterNot(n => evictions.exists(_.old == n))
        .sortBy(_.value)

    private lazy val arrowsMap: Map[Node, Node] =
      arrows.map(a => a.to -> a.from).toMap

    def pathToRoot(node: Node): Seq[Node] = {
      @tailrec
      def go(node: Node, acc: Seq[Node], seen: Set[Node]): Seq[Node] = {
        val acc2 = acc :+ node
        arrowsMap.get(node) match {
          case Some(n) if !seen.contains(n) => go(n, acc2, seen + node)
          case _    => acc2
        }
      }
      go(node, Seq.empty, Set.empty)
    }
  }

  object DependencyGraph {
    def empty =
      DependencyGraph(
        nodes     = Set.empty,
        arrows    = Set.empty,
        evictions = Set.empty
      )
  }
}
