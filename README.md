#Overview
This is an Sbt plugin that can be used to ensure up-to-date dependencies are being used in an SBT project.

#Background
It can be hard to ensure that distributed teams upgrade to the latest version of a dependency. This is a problem when there are security fixes or other reasons to require a library to be upgraded. Bobby provides the capability to fail builds which have out-of-date dependencies. Ideally communications will be in place to ensure updates happen but Bobby acts as a safety net of last resort.

Currently version 0.2.3 checks your projects' dependency versions against the lastest available.

Bobby will your nexus credentials in /.sbt/.credentials.

#How To Use (currently influx)

In your "~/.sbt/0.13/plugins/build.sbt"

set
```
addSbtPlugin("uk.gov.hmrc" % "bobby" % "0.2.3")
```





