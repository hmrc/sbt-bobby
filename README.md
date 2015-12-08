[![Join the chat at https://gitter.im/hmrc/sbt-bobby](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hmrc/sbt-bobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/hmrc/sbt-bobby.svg)](https://travis-ci.org/hmrc/sbt-bobby) [ ![Download](https://api.bintray.com/packages/hmrc/sbt-plugin-releases/sbt-bobby/images/download.svg) ](https://bintray.com/hmrc/sbt-plugin-releases/sbt-bobby/_latestVersion) [![Stories in Ready](https://badge.waffle.io/hmrc/sbt-bobby.png?label=ready&title=Ready)](https://waffle.io/hmrc/sbt-bobby)

#Overview

Bobby is an Sbt plugin that prevents outdated dependencies from being used by your project.

#Background
It can be hard to ensure that distributed teams upgrade to the latest version of a dependency. 
This is a problem when there are security fixes or other reasons to require a library to be upgraded. 

Bobby provides the capability to fail builds which have outdated dependencies. 
Ideally communications will be in place to ensure updates happen but Bobby acts as a safety net of last resort.

Bobby also checks your projects' dependency versions against the latest available.
If a newer one is available it suggests to use it without failing the build.
The current version looks into nexus for this, using what is defined in ~/.sbt/.credentials. 
If undefined it skips this step

#How To Use

In your `~/.sbt/0.13/plugins/build.sbt`, set:
```scala
resolvers += Resolver.url(
  "hmrc-sbt-plugin-releases",
    url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-bobby" % "[INSERT-VERSION]")
```

Then call the 'validate' command:

```sbt validate```

#### Outdated Dependencies

To prevent outdated dependencies from being used by your project, create a blacklist of version ranges. 
For example:

```json
[
  {
    "organisation": "uk.gov.hmrc",
    "name": "my-library",
    "range": "(,6.0.0)",
    "reason": "Versions older than 6.0.0 have a security vulnerability",
    "from": "2015-03-15"
  },
  {
    "organisation": "uk.gov.hmrc",
    "name": "my-other-library",
    "range": "[1.2.0]",
    "reason": "1.2.0 has a bug",
    "from": "2015-03-15"
  },
  {
    "organisation": "*",
    "name": "*",
    "range": "[*-SNAPSHOT]",
    "reason": "You shouldn't be deploying a snapshot to production should you?",
    "from": "2000-01-01"
  }
]
```

Tell Bobby where to find the file containing the list by setting a `deprecated-dependencies` property in `~/.sbt/bobby.conf`. Bobby can read both local or remote files:

```properties
deprecated-dependencies = https://some-url/deprecated-dependencies.json
deprecated-dependencies = file:///~/.sbt/deprecated-dependencies.json
```


###### Blacklist:
The blacklist must be a json with a list of rows where:
* _organisation_ and _name_ identify the dependency. You can use '*' as wildcard
* _range_ is used to declare minimum, maximum allowed versions of a dependency (both min and max may be optional), and allow "holes" for known incompatible versions. See 'Supported Version Ranges' for more details
* _reason_ tells why the versions in range are deprecated
* _from_ tells when the versions in range become unsupported. The builds will fail after that day. Before only a warning is shown.


###### Supported Version Ranges
| Range          | Meaning                               |
|----------------|---------------------------------------|
| (,1.0.0]       | x <= 1.0.0                            |
| [1.0.0]        | Hard requirement on 1.0.0             |
| [1.2.0,1.3.0]  | 1.2.0 <= x <= 1.3.0                   |
| [1.0.0,2.0.0)  | 1.0.0 <= x < 2.0.0                    |
| [1.5.0,)       | x >= 1.5.0                            |
| [*-SNAPSHOT]   | Any version with qualifier 'SNAPSHOT' |

###### JSON output file
Bobby can be configured to output results to a structured JSON file. To enable this feature provide an output filepath using the 
optional output-file parameter in your ~/.sbt/bobby.conf file. The file path could be relative or absolute.

```properties
output-file = bobby-output.json
```

## Developing ##

* The TestProject is useful for executing the SbtBobby plugin from source against a test project. CD into the directory 
and run ```sbt validate```
 
* Bobby uses [scripted](http://eed3si9n.com/testing-sbt-plugins) tests which are executed with ```sbt scripted```

## License ##
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

