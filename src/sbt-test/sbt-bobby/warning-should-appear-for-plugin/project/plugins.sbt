
resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(
Resolver.ivyStylePatterns)

// This dependency being present should cause a warning from bobby
addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.5.0")

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("uk.gov.hmrc" % "sbt-bobby" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.bobbyRulesURL
bobbyRulesURL := Some(file("bobby-rules.json").toURI.toURL)
