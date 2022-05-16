
lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name             := "sbt-bobby",
    sbtPlugin        := true,
    majorVersion     := 4,
    isPublicArtefact := true,
    scalaVersion     := "2.12.10",
    crossSbtVersions := Vector("1.3.13"),
    // Use the code from the sbt-dependency-graph plugin as if it was a standard library dependency
    // We use the plugin to resolve the complete module graph for the purpose of validating bobby
    // rule violations across transitive dependencies
    addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1"),
    libraryDependencies ++= Seq(
      "commons-codec"         %  "commons-codec"              % "1.14",
      "com.lihaoyi"           %% "fansi"                      % "0.2.6",
      "com.typesafe.play"     %% "play-json"                  % "2.9.2",
      "org.scalatest"         %% "scalatest"                  % "3.1.0"         % Test,
      "com.vladsch.flexmark"  %  "flexmark-all"               % "0.35.10"       % Test,
      "org.scalacheck"        %% "scalacheck"                 % "1.14.3"        % Test,
      "org.scalatestplus"     %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"   % Test
    ),
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
