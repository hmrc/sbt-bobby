[![Join the chat at https://gitter.im/hmrc/sbt-bobby](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hmrc/sbt-bobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/hmrc/sbt-bobby.svg)](https://travis-ci.org/hmrc/sbt-bobby) [ ![Download](https://api.bintray.com/packages/hmrc/sbt-plugin-releases/sbt-bobby/images/download.svg) ](https://bintray.com/hmrc/sbt-plugin-releases/sbt-bobby/_latestVersion) [![Stories in Ready](https://badge.waffle.io/hmrc/sbt-bobby.png?label=ready&title=Ready)](https://waffle.io/hmrc/sbt-bobby)

# Overview

Bobby is an SBT plugin that prevents outdated dependencies and plugins from being used by your project.

# Background
It can be hard to ensure that distributed teams do not use versions of dependencies that may contain bugs or security flaws. Bobby provides the capability to fail builds which reference such outdated dependencies. Ideally communications will be in place to ensure updates happen but Bobby acts as a safety net of last resort.

Bobby also checks your projects' dependency versions against the latest available.
If a newer one is available it suggests to use it without failing the build.
The current version looks into nexus for this, using what is defined in ~/.sbt/.credentials. 
If undefined it skips this step

# When will Bobby fail a build?

Bobby is automatically run for you on ci-dev, there is no need to explicitly add it to your project. Bobby works by checking against a blacklist of dependencies (known as rules), and will fail a build if it finds any dependencies in your build that match the versions/version ranges in this list.

Bobby will write out a summary table to the console, as well as generating report artifcats ```bobby-report.json``` and ```bobby-report.html```. This report tells you of any rule violations that are preventing your job from building, as well as highlight any dependencies that are not on the latest version that you should optionally upgrade.

An example output looks like this:

```+-------+------------------------------------+--------------+----------------+------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Level | Dependency                         | Your Version | Latest Version | Deadline   | Information                                                                                                                                                       |
+-------+------------------------------------+--------------+----------------+------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| WARN  | uk.gov.hmrc.simple-reactivemongo   | 2.1.0        | 4.3.0          | 2015-11-30 | ReactiveMongo upgrade. Please consider upgrading to '4.3.0' |
| WARN  | uk.gov.hmrc.play-reactivemongo     | 3.1.0        | 4.4.0          | 2015-11-30 | ReactiveMongo upgrade. Please consider upgrading to '4.4.0'   |
| WARN  | uk.gov.hmrc.microservice-bootstrap | 2.0.0        | 3.0.0          | 2015-11-23 | Auditing upgrade. Please consider upgrading to '3.0.0'    |
| INFO  | uk.gov.hmrc.simple-reactivemongo   | 2.1.0        | 4.3.0          | -          | 'simple-reactivemongo 2.1.0' is not the most recent version, consider upgrading to '4.3.0'                                                                                        |
| INFO  | uk.gov.hmrc.play-reactivemongo     | 3.1.0        | 4.4.0          | -          | 'play-reactivemongo 3.1.0' is not the most recent version, consider upgrading to '4.4.0'                                                                                          |
| INFO  | uk.gov.hmrc.play-health            | 0.7.0        | 1.1.0          | -          | 'play-health 0.7.0' is not the most recent version, consider upgrading to '1.1.0'                                                                                                 |
| INFO  | uk.gov.hmrc.play-config            | 2.0.0        | 2.0.1          | -          | 'play-config 2.0.0' is not the most recent version, consider upgrading to '2.0.1'                                                                                                 |
| INFO  | uk.gov.hmrc.microservice-bootstrap | 2.0.0        | 3.0.0          | -          | 'microservice-bootstrap 2.0.0' is not the most recent version, consider upgrading to '3.0.0'                                                                                      |
| INFO  | uk.gov.hmrc.domain                 | 2.11.0       | 3.1.0          | -          | 'domain 2.11.0' is not the most recent version, consider upgrading to '3.1.0'                                                                                                     |
| INFO  | org.scalatest.scalatest            | 2.2.1        | 2.2.2          | -          | 'scalatest 2.2.1' is not the most recent version, consider upgrading to '2.2.2'                                                                                                   |
+-------+------------------------------------+--------------+----------------+------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```

# How are rules configured?

Bobby config is an array of JSON objects of the form:
```
   [
     { 
       "organisation" : "uk.gov.hmrc", 
       "name"         : "sbt-auto-build", 
       "range"        : "(,0.8.0)", 
       "reason"       : "Previous versions didn't work correctly", 
       "from"         : "2015-06-08" 
     }
   ]
```

###### Where:
* _organisation_ and _name_ identify the dependency
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

# How do I change the rules used by Jenkins?

On Jenkins, Bobby sources it's config remotely. Current rules can be found in the following project https://github.com/hmrc/bobby-config (available only for people in the HMRC org on github). 

Anyone working on the Tax Platform can add/change bobby rules. We accept pull requests to both bobby config repositories, and once merged the new rules will take effect immediately. 

An example commit is as follows. Note that we should always try to stick to one rule per dependency. https://github.com/hmrc/bobby-config/commit/f1b1b180cde857d64e3b1c2fb5322bc400c18c8a

# How to use Bobby on your local builds

### Sbt 1.x

Since major version 1, this plugin is cross compiled for sbt 1.x (specifically 1.3.4).

| Sbt version | Plugin version |
| ----------- | -------------- |
| `0.13.x`    | `any`          |
| `>= 1.x`    | `>= 1.x`       |

In your project/plugins.sbt file:

```scala
resolvers += Resolver.url(
  "hmrc-sbt-plugin-releases",
    url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-bobby" % "[INSERT-VERSION]")
```

Then in your `build.sbt` you need to tell `sbt-bobby` where the configuration rules are.

First add the required imports:
```
import java.net.URL
import SbtBobbyPlugin.BobbyKeys.deprecatedDependenciesUrl
```
Then point to the current config:
```
deprecatedDependenciesUrl := Some(new URL("path to your bobby rules file")),
```

Then call the 'validate' command:

```sbt validate```

> Alternatively you can add a `bobby.conf` file and set the location there: 
> `deprecated-dependencies=path to your bobby rules file`

# Providing custom rules

If you want to prevent outdated dependencies from being used by your project outside of HMRC, you can create a blacklist of version ranges in the following format:

```json
{
    "libraries" :[
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
    ],
    "plugins" : [
      {
          "organisation": "uk.gov.hmrc",
          "name": "sbt-auto-build",
          "range": "[1.0.0]",
          "reason": "1.0.0 has a bug",
          "from": "2099-01-01"
       }
    ]
}
```

Once this is done, tell Bobby where to find the file containing the list by setting a `deprecated-dependencies` property in `~/.sbt/bobby.conf`. Bobby can read both local or remote files:

```
deprecated-dependencies = https://some-url/deprecated-dependencies.json
deprecated-dependencies = file:///~/.sbt/deprecated-dependencies.json
```

###### JSON output file
Bobby can be configured to output results to a structured JSON file. To enable this feature provide an output filepath using the 
optional output-file parameter in your ~/.sbt/bobby.conf file. The file path could be relative or absolute.

```
output-file = bobby-output.json
```

## Developing ##

* The test-project folder contains a simple test project is useful for executing the sbt-bobby plugin from source against a test project. CD into the directory and run ```sbt validate```. It can also be debugged by doing ```sbt -jvm-debug 5005 validate```
 
* Bobby uses [scripted](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html) tests which are executed with ```sbt scripted```

## License ##
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

