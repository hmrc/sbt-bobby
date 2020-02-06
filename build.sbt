val pluginName = "sbt-bobby"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 2,
    makePublicallyAvailableOnBintray := true
  )
  .settings(
    sbtPlugin := true,
    name := pluginName,
    scalaVersion := "2.12.10",
    crossSbtVersions := Vector("0.13.18", "1.3.4"),
    // Use the code from the sbt-dependency-graph plugin as if it was a standard library dependency
    // We use the plugin to resolve the complete module graph for the purpose of validating bobby
    // rule violations across transitive dependencies
    addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1"),
    libraryDependencies ++= Seq(
      "commons-codec"         % "commons-codec"               % "1.14",
      "com.lihaoyi"           %% "fansi"                      % "0.2.6",
      "com.typesafe.play"     %% "play-json"                  % "2.6.14", // Kept lower for sbt 0.13 compatibility
      "org.joda"              % "joda-convert"                % "2.2.1", //Required only to prevent warnings with play-json (https://stackoverflow.com/questions/13856266/class-broken-error-with-joda-time-using-scala)
      "org.scalatest"         %% "scalatest"                  % "3.1.0"         % Test,
      "com.vladsch.flexmark"  % "flexmark-all"                % "0.35.10"       % Test,
      "org.scalacheck"        %% "scalacheck"                 % "1.14.3"        % Test,
      "org.scalatestplus"     %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"   % Test
    ),
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
