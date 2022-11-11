
lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name             := "sbt-bobby",
    sbtPlugin        := true,
    majorVersion     := 4,
    isPublicArtefact := true,
    scalaVersion     := "2.12.17",
    crossSbtVersions := Vector("1.6.2"),
    libraryDependencies ++= Seq(
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
