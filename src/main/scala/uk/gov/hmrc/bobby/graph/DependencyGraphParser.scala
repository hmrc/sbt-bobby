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

package uk.gov.hmrc.bobby.graph

import scala.annotation.tailrec

object DependencyGraphParser {

  def parse(input: String): DependencyGraph = {
    val graph = lexer(input.split("\n").toIndexedSeq)
      .foldLeft(DependencyGraphWithEvictions.empty) { (graph, t)  =>
        t match {
          case n: Node     => graph.copy(nodes     = graph.nodes      + n)
          case a: Arrow    => graph.copy(arrows    = graph.arrows     + a)
          case e: Eviction => graph.copy(evictions = graph.evictions  + e)
        }
      }

    // maven generated digraphs don't add nodes, but the main dependency is named the same as the digraph
    if (graph.nodes.isEmpty) {
      val nodesFromRoot = graph.arrows.map(_.to) ++ graph.arrows.map(_.from)
      graph.copy(nodes = nodesFromRoot).applyEvictions
    } else
      graph.applyEvictions
  }

  private val eviction = """\s*"(.+)" -> "(.+)" \[(.+)\]\s*""".r
  private val arrow    = """\s*"(.+)" -> "(.+)"\s*;?\s*""".r
  private val node     = """\s*"(.+)"\[(.+)\]\s*""".r

  private def lexer(lines: Seq[String]): Seq[Token] =
    lines.flatMap {
      case node(n, _)        => Some(Node(n))
      case arrow(a, b)       => Some(Arrow(Node(a), Node(b)))
      case eviction(a, b, r) => Some(Eviction(Node(a), Node(b), r))
      case _                 => None
    }

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
    def scalaVersion = Option(sv)

    def toModuleID =
      sbt.ModuleID(organization = group, name = artefact, revision = version)
  }

  case class Arrow(from: Node, to: Node) extends Token

  case class Eviction(old: Node, by: Node, reason: String) extends Token

  // This is the uninterpretted graph data
  // call `applyEvictions` to filter evicted nodes out
  case class DependencyGraphWithEvictions(
    nodes    : Set[Node],
    arrows   : Set[Arrow],
    evictions: Set[Eviction]
  ) {
    def applyEvictions: DependencyGraph = {
      val evictedNodes = evictions.map(_.old)
      DependencyGraph(
        nodes  = nodes.filterNot(n => evictedNodes.contains(n)),
        arrows = arrows.filterNot(a => evictedNodes.contains(a.from) || evictedNodes.contains(a.to))
      )
    }
  }

  object DependencyGraphWithEvictions {
    def empty: DependencyGraphWithEvictions =
      DependencyGraphWithEvictions(
        nodes     = Set.empty,
        arrows    = Set.empty,
        evictions = Set.empty
      )
  }

  case class DependencyGraph(
    nodes : Set[Node],
    arrows: Set[Arrow]
  ) {
    def dependencies: Seq[Node] =
      nodes
        .toList
        .sortBy(_.value)

    def pathToRoot(node: Node): Seq[Node] =
      // return shortest path (if multiple)
      pathsToRoot(node).sortBy(_.length).head

    def pathsToRoot(node: Node): Seq[Seq[Node]] =
      tailRecM((node, Seq.empty[Node])) {
        case (n, path) =>
          arrows
            .filter(_.to == n)
            .toSeq match {
              case Nil => Seq(Right(path :+ n))
              case _ if path.contains(n) => Seq(Right(path)) // handle cyclical dependencies
              case xs  => xs.map(x => Left((x.from, path :+ n)))
            }
      }

    // lifted from cats
    def tailRecM[A, B](a: A)(fn: A => Seq[Either[A, B]]): Seq[B] = {
      val buf = Seq.newBuilder[B]
      var state = List(fn(a).iterator)
      @tailrec
      def loop(): Unit =
        state match {
          case Nil => ()
          case h :: tail if h.isEmpty =>
            state = tail
            loop()
          case h :: tail =>
            h.next() match {
              case Right(b) =>
                buf += b
                loop()
              case Left(a) =>
                state = (fn(a).iterator) :: h :: tail
                loop()
            }
        }
      loop()
      buf.result()
    }

    lazy val root: Node = {
      // we should be able to return the last node of any path - but we sometimes get strange graphs with orphan dependencies
      // here we filter them out by returning the most common root
      val roots = for {
        node <- nodes.toSeq
        path <- pathsToRoot(node)
      } yield
        path.last
      val uniqueRoots = roots.groupBy(identity).mapValues(_.size)
      if (uniqueRoots.size > 1)
        sbt.ConsoleLogger().warn(s"Multiple roots found: ${uniqueRoots}")
      uniqueRoots.toSeq.sortBy(_._2).last._1
    }
  }

  object DependencyGraph {
    def empty: DependencyGraph =
      DependencyGraph(
        nodes  = Set.empty,
        arrows = Set.empty
      )
  }
}
