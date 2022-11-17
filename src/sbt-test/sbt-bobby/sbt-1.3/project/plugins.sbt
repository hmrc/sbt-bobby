resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("uk.gov.hmrc" % "sbt-bobby" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys.bobbyRulesURL
bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
