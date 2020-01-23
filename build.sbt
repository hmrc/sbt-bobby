val pluginName = "sbt-bobby"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 1,
    makePublicallyAvailableOnBintray := true
  )
  .settings(
    sbtPlugin := true,
    name := pluginName,
    scalaVersion := "2.12.10",
    crossSbtVersions := Vector("0.13.18", "1.3.4"),
    libraryDependencies ++= Seq(
      "commons-codec"         % "commons-codec" % "1.14",
      "joda-time"             % "joda-time"     % "2.10.5",
      "org.joda"              % "joda-convert"  % "2.2.1",
      "com.typesafe.play"     %% "play-json"    % "2.6.14"  % Test,
      "org.scalatest"         %% "scalatest"    % "3.1.0"   % Test,
      "com.vladsch.flexmark"  % "flexmark-all"  % "0.35.10" % Test
    ),
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
