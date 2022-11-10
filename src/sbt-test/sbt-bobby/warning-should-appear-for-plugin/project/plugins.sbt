resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

// This dependency being present should cause a warning from bobby
addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.5.0")

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("uk.gov.hmrc" % "sbt-bobby" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys.bobbyRulesURL
bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
