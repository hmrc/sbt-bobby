resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-bobby"      % sys.props("project.version"))
addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.0.0")
