/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.typesafe.sbt.SbtGit.git
import sbt._
import Keys._

object PluginBuild extends Build {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
  import com.typesafe.sbt.GitVersioning
  import com.typesafe.sbt.SbtGit.git

  val pluginName = "sbt-bobby"

  lazy val root = Project(pluginName, base = file("."), settings =
    Seq(
    sbtPlugin := true,
    organization := "uk.gov.hmrc",
    name := pluginName,
    scalaVersion := "2.10.4",
    resolvers ++= Seq(
      Opts.resolver.sonatypeReleases,
      Opts.resolver.sonatypeSnapshots
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.4.0-M1",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "org.pegdown" % "pegdown" % "1.5.0" % "test"
    ),
    publishArtifact := true,
    publishArtifact in Test := false,
    HeaderSettings()
  ) ++ Git() ++ ArtefactDescription() ++ defaultSettings()
  ).enablePlugins(AutomateHeaderPlugin, GitVersioning)
}

object Git {
  def apply() = Seq(
    git.useGitDescribe := true,
    git.versionProperty := "NONE",
    git.gitDescribedVersion <<= git.gitDescribedVersion((v) => v.map(_.drop(1)))
  )
}

object ArtefactDescription {

  import scala.xml.transform.{RewriteRule, RuleTransformer}
  import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}

  def apply() = Seq(
      organizationHomepage := Some(url("https://www.gov.uk/government/organisations/hm-revenue-customs")),
      homepage := Some(url("https://github.com/hmrc/sbt-bobby")),
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      scmInfo := Some(
        ScmInfo(
          url("https://github.com/hmrc/sbt-bobby"),
          "git@github.com:hmrc/sbt-bobby.git"
        )
      ),
      developers := List(
          Developer("jakobgrunig", "Jakob Grunig", "jgrunig@equalexperts.com", url("http://www.equalexperts.com")),
          Developer("charleskubicek", "Charles Kubicek", "ckubicek@equalexperts.com", url("http://www.equalexperts.com")),
          Developer("duncancrawford", "Duncan Crawford", "dcrawford@equalexperts.com", url("http://www.equalexperts.com"))
        ),

      // workaround for sbt/sbt#1834
      pomPostProcess := { (node: XmlNode) =>
        new RuleTransformer(new RewriteRule {
          override def transform(node: XmlNode): XmlNodeSeq = node match {
            case e: Elem
              if e.label == "developers" =>
              <developers>
                {developers.value.map { dev =>
                <developer>
                  <id>{dev.id}</id>
                  <name>{dev.name}</name>
                  <email>{dev.email}</email>
                  <url>{dev.url}</url>
                </developer>
              }}
              </developers>
            case _ => node
          }
        }).transform(node).head
      }
    )

}

object HeaderSettings {
  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import de.heikoseeberger.sbtheader.license.Apache2_0

  def apply() = headers := Map("scala" -> Apache2_0("2015", "HM Revenue & Customs"))
}
