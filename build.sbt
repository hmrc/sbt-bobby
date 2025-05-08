
lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name             := "sbt-bobby",
    sbtPlugin        := true,
    majorVersion     := 5,
    isPublicArtefact := true,
    scalaVersion     := "2.12.20",
    crossSbtVersions := Vector("1.10.10"),
    libraryDependencies ++= Seq(
      "com.lihaoyi"           %% "fansi"                      % "0.4.0",
      "com.typesafe.play"     %% "play-json"                  % "2.9.4",
      "org.scalatest"         %% "scalatest"                  % "3.2.19"        % Test,
      "com.vladsch.flexmark"  %  "flexmark-all"               % "0.64.8"        % Test,
      "org.scalacheck"        %% "scalacheck"                 % "1.18.1"        % Test,
      "org.scalatestplus"     %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"   % Test
    ),
    scriptedLaunchOpts := {
      val homeDir = sys.props.get("jenkins.home")
                             .orElse(sys.props.get("user.home"))
                             .getOrElse("")
      scriptedLaunchOpts.value ++
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + version.value,
        s"-Dsbt.override.build.repos=${sys.props.getOrElse("sbt.override.build.repos", "false")}",
        // s"-Dsbt.global.base=$sbtHome/.sbt",
        // Global base is overwritten with <tmp scripted>/global and can not be reconfigured
        // We have to explicitly set all the params that rely on base
        s"-Dsbt.boot.directory=${file(homeDir)          / ".sbt" / "boot"}",
        s"-Dsbt.repository.config=${file(homeDir)       / ".sbt" / "repositories"}",
        s"-Dsbt.boot.properties=file:///${file(homeDir) / ".sbt" / "sbt.boot.properties"}",
      )
    },
    scriptedBufferLog := false
  )
