#Overview
This is an Sbt plugin that can be used to enforce (fail compilation) or warn when libraries and dependencies need to be upgraded.

#Background
'Bobby' is British slang for a police officer

It can be hard to ensure that distributed teams upgrade to the newest (mandatory) version of a library. Occasionally there may be security fixes or other reasons to require a library to be upgraded. 

#How To Use

In your "~/.sbt/0.13/plugins/build.sbt"

set
```
addSbtPlugin("uk.gov.hmrc" % "bobby" % "0.2.1")
```

In your "~/sbt/0.13/global.sbt"

set
```
val bobbyNexus = "https://some.nexus/service/local/lucene/"

val mandatoryReleases = Seq(
   ("org.scala-lang", "scala-library", "2.11.8"),
   ("org.scala-lang", "scala-compiler", "6.9.25")
)
```




