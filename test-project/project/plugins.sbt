val location = file("..").toURI

val sbtBobby = RootProject(location)

val root = project.in(file(".")).dependsOn(sbtBobby)
