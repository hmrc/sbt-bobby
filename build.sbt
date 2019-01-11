import uk.gov.hmrc.DefaultBuildSettings.targetJvm

val pluginName = "sbt-bobby"

lazy val root = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 0,
    makePublicallyAvailableOnBintray := true
  )
  .settings(
    sbtPlugin := true,
    name := pluginName,
    targetJvm := "jvm-1.7",
    scalaVersion := "2.10.4",
    libraryDependencies ++= Seq(
      "commons-codec"     % "commons-codec" % "1.10",
      "joda-time"         % "joda-time"     % "2.9.1",
      "org.joda"          % "joda-convert"  % "1.8.1",
      "com.typesafe.play" %% "play-json"    % "2.3.10" % Test,
      "org.scalatest"     %% "scalatest"    % "2.2.4" % Test,
      "org.pegdown"       % "pegdown"       % "1.5.0" % Test
    )
  )
  .settings(ScriptedPlugin.scriptedSettings: _*)
  .settings(scriptedLaunchOpts += s"-Dproject.version=${version.value}")
