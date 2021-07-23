[![Join the chat at https://gitter.im/hmrc/sbt-bobby](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hmrc/sbt-bobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) 

## Overview

<img src="https://github.com/hmrc/sbt-bobby/blob/master/images/bobby.jpg" alt="Bobby">

Bobby, a.k.a. your friendly neighbourhood build policeman, is an SBT plugin that prevents outdated dependencies and plugins from being used by your project.

You create a set of rules which outlaw particular versions of a library or plugin, and task Bobby to enforce those rules. 
If a violation is detected, the whistle is blown, and the build is failed.
 
Bobby can help your team block a bad dependency with a known security issue or memory leak for example, or simply to enforce that
libraries are upgraded.

<img src="https://github.com/hmrc/sbt-bobby/blob/master/images/bobby-output.png" alt="Bobby Output">

## Background
It can be hard to ensure that distributed teams do not use versions of dependencies that are known to contain bugs or security flaws. 

If a new bug is found in a library and it should be outlawed across your platform, it can be difficult to ensure upgrades happen. 

Using Bobby, this can be enforced by adding a simple rule which outlaws that bad dependency, possibly future dated to give some leadtime to 
perform the update.

Ideally communications will be in place to ensure updates happen organically but Bobby acts as a safety net of last resort. 

## How does Bobby work?

Bobby inspects the build and pulls out all of the dependencies you've declared, and all the transitive dependencies that those pull in. 

It will then check each against the set of rules and tag them as either:

* _BobbyViolation_ => A dependency that has been outlawed, is active now, and will cause the build to fail with an exception. This must be urgently fixed before the build can be allowed to continue
* _BobbyWarning_ => A dependency that will become outlawed from a given date in the future. It will not fail the build today, but will become a 
BobbyViolation from the specified date, so should be looked at with high priority
* _BobbyOk_ => A dependency that is clean and has no rules against it. No issue to take

## Creating a Bobby Rule

Bobby Rules are defined in a single `json` file, and look like this:

```
{
  "libraries": [
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

Rules can be placed on both libraries and plugins, and will be enforced on all local and transitive dependencies.

>Note that as of version 3.2.0 the two lists are merged together by the plugin and used as one. They are only divided into two to preserve
>backwards compatiblity with previous releases of `sbt-bobby`

> In order to apply Bobby to the meta scope to validate plugin output, see the section below on 'Running in different configurations'

## Rule Schema
Each rule takes the same form:
```
{
  "organisation": "com.typesafe.play",
  "name": "sbt-plugin",
  "range": "(,2.5.19)",
  "reason": "Critical security upgrade",
  "from": "2019-03-04"
}
```
Where:
* `organisation` and `name` identify the dependency
* `range` is used to target minimum and maximum versions of a dependency (both min and max may be optional), and allow "holes" for known incompatible versions. See 'Supported Version Ranges' for more details
* `reason` is a short descriptive message to explain why the versions matching the range are outlawed
* `from` is the date the rule will come into effect. The builds will fail after that day, and generate a warning up to it

## How to setup and trigger Bobby?

Bobby should *not* be added to an individual projects build. Instead, it should be added as a global plugin

This means:
* All projects can benefit from Bobby
* Bobby can be updated centrally

>At HMRC bobby is added on the Jenkins build-servers and runs automatically for you. You do not need to do anything, unless you
>wish to also run it locally

Just add the plugin to `./sbt/<version>/plugins/plugins.sbt`:
```
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-bobby" % "[INSERT-VERSION]")
```

Then, create your rules configuration as above. This file can live anywhere, you just need to tell Bobby where to find it.

This can be done by setting a `bobby-rules-url` property in `~/.sbt/bobby.conf`. Bobby can read both local or remote files:

```
bobby-rules-url = https://some-url/deprecated-dependencies.json
bobby-rules-url = file:///~/.sbt/deprecated-dependencies.json
```

That's it!

Now you can run bobby with `sbt validate`. 

If your build is making use of any outlawed dependencies, an exception will be thrown. 
Otherwise, all is good and you can carry on with your day.

> See the 'Configuration Options' section below for more configuration options

### Running in different configurations

Bobby respects the configuration scoping mechanisms built into sbt, which means you can run:

`sbt validate` to validate _compile_ dependencies 
`sbt test:validate` to validate _test_ dependencies
`sbt "reload plugins; validate; reload return"` to validate _plugin_ dependencies

There is also a helper command alias which runs all three of these in one:

`sbt validateAll`

>Prior to major version 3, Bobby tried to pull out both local and plugin dependencies in one task. This has been
>changed to better integrate with the sbt [scoping](https://www.scala-sbt.org/1.x/docs/Scopes.html) ecosystem 

### Sbt 1.x
 
 Since major version 1, this plugin is cross compiled for sbt 1.x (specifically 1.3.4).
 
 | Sbt version | Plugin version |
 | ----------- | -------------- |
 | `0.13.x`    | `any`          |
 | `>= 1.x`    | `>= 1.x`       |

## Supported Version Ranges

The range that is outlawed can be configured using Ivy style syntax. 

| Range          | Applies to                            |
|----------------|---------------------------------------|
| (,1.0.0]       | x <= 1.0.0                            |
| [1.0.0]        | Hard requirement on 1.0.0             |
| [1.2.0,1.3.0]  | 1.2.0 <= x <= 1.3.0                   |
| [1.0.0,2.0.0)  | 1.0.0 <= x < 2.0.0                    |
| [1.5.0,)       | x >= 1.5.0                            |
| [*-SNAPSHOT]   | Any version with qualifier 'SNAPSHOT' |

Using these ranges you can easily specify minimum and maximum ranges, or exact versions to outlaw.

## Rule precedence

Sometimes you might have multiple rules that apply for a given dependency. Maybe you create a rule to outlaw
version `1.2.0` of `myawesomelib` because it has a known memory leak. Later on, you might decide to blanket ban all
versions <= `2.0.0`. You would then have two rules with ranges:

```
1. [1.2.0]      Effective from T+1  (so a BobbyWarning )
2. (,2.0.0]     Effective from T-1  (so a BobbyViolation )
```
If a build then has a dependency on `myawesomelib % 1.2.0` then both rules match. We clearly want to apply the second rule
that is already in effect and results in a violation, over the first that would allow the build to continue with only a 
warning.

So in order to disambiguate multiple rules, first Bobby partitions them to only consider violations first. If none match,
it will consider warnings. In each subset, the ordering is (in decreasing ordering of precedence):

1. First consider the upper bound of the rule version. The one that is the *highest* takes precedence
   1. No upper bound (None) first
   1. Highest upper bound if both defined
2. Inclusive upper bound over exclusive (when version numbers matching)
3. Most recent rule first
4. Undefined (very much an edge case, pick any)

Precedence is in this order so that a blanket ban across a range takes effect over a ban on a single version. The reasoning
behind this is in the case where you had a specific rule on versions V1, V2, V3 and V1-3. If a build tries to use V1,
the preference is to show them it is blocked because of the V1-3 rule, saving them trying upgrading to V2, then V3 first and getting 
a different violation each time.

## Understanding the Bobby output

Bobby will write out a summary table to the console, as well as generating two report artifacts for every project/configuration scope:
 
 * `target/bobby-report-<project>-<configuration>.json` 
 * `target/bobby-report-<project>-<configuration>.json`
 
For example, if you are running `test:validate` from a project called `root`, the files generated will be:

 * `target/bobby-report-root-test.json` 
 * `target/bobby-report-root-test.json`
  
These reports tell you of any rule violations that are preventing your job from building, as well as 
highlighting any dependencies with warnings that will become violations in the future.

An example output looks like this (a snippet taken from the `test-project` in this repo, see below):

```
[info] ************************************************************************************************************************
[info] Level KEY:
[info]  * ERROR: Bobby Violations => Your build will forcibly fail if any violations are detected
[info]  * WARN: Bobby Warnings => Your build will start to fail from the date the rules become enforced
[info]  * INFO: Bobby Ok => No problems with this dependency
[info]
[info] Dependency KEY:  validate 0s
[info]  * L: Local Dependency => Highlights dependencies declared locally in your project
[info]  * T: Transitive Dependency => Dependencies pulled in via your locally declared dependencies
[info] ************************************************************************************************************************
[info] +-------+--------------------------------------------------------+----------------------------------+----------------+----------------+----------------+
[info] | Level | Dependency                                             | Via                              | Your Version   | Outlawed Range | Effective From |
[info] +-------+--------------------------------------------------------+----------------------------------+----------------+----------------+----------------+
[info] | ERROR | org.scalatest.scalatest                                |                                  | 3.0.0          | (,3.1.0)       | 2020-01-01     |
[info] | ERROR | uk.gov.hmrc.simple-reactivemongo                       |                                  | 7.13.0-play-26 | [7.0.0,7.14.0] | 2020-01-01     |
[info] | WARN  | org.pegdown.pegdown                                    |                                  | 1.3.0          | [0.0.0-0.0.0,) | 2099-01-01     |
[info] | INFO  | aopalliance.aopalliance                                | uk.gov.hmrc.simple-reactivemongo | 1.0            | -              | -              |
[info] | INFO  | com.fasterxml.jackson.core.jackson-annotations         | uk.gov.hmrc.simple-reactivemongo | 2.8.11         | -              | -              |
[info] | INFO  | com.fasterxml.jackson.core.jackson-core                | uk.gov.hmrc.simple-reactivemongo | 2.8.11         | -              | -              |
[info] | INFO  | com.fasterxml.jackson.core.jackson-databind            | uk.gov.hmrc.simple-reactivemongo | 2.8.11.1       | -              | -              |
[info] | INFO  | com.fasterxml.jackson.datatype.jackson-datatype-jdk8   | uk.gov.hmrc.simple-reactivemongo | 2.8.11         | -              | -              |
[info] | INFO  | com.fasterxml.jackson.datatype.jackson-datatype-jsr310 | uk.gov.hmrc.simple-reactivemongo | 2.8.11         | -              | -              |
[info] | INFO  | com.github.nscala-time.nscala-time                     | uk.gov.hmrc.simple-reactivemongo | 2.22.0         | -              | -              |
[info] | INFO  | com.google.code.findbugs.jsr305                        | uk.gov.hmrc.simple-reactivemongo | 1.3.9          | -              | -              |
[info] | INFO  | org.scala-lang.scala-reflect                           | uk.gov.hmrc.simple-reactivemongo | 2.12.10        | -              | -              |
[info] | INFO  | org.scala-stm.scala-stm                                | uk.gov.hmrc.simple-reactivemongo | 0.8            | -              | -              |
[info] | INFO  | org.scalactic.scalactic                                | org.scalatest.scalatest          | 3.0.0          | -              | -              |
[info] | INFO  | org.slf4j.jcl-over-slf4j                               | uk.gov.hmrc.simple-reactivemongo | 1.7.25         | -              | -              |
[info] | INFO  | org.slf4j.jul-to-slf4j                                 | uk.gov.hmrc.simple-reactivemongo | 1.7.25         | -              | -              |
[info] | INFO  | org.slf4j.slf4j-api                                    | uk.gov.hmrc.simple-reactivemongo | 1.7.25         | -              | -              |
[info] | INFO  | org.typelevel.macro-compat                             | uk.gov.hmrc.simple-reactivemongo | 1.1.1          | -              | -              |
[info] +-------+--------------------------------------------------------+----------------------------------+----------------+----------------+----------------+
[warn] WARNING: Your build has 1 bobby warning(s). Please take action to fix these before the listed date, or they will become violations that fail your build
[warn]  (1) org.pegdown.pegdown (1.3.0)
[warn]      Reason: Example: No pegdown dependencies will be allowed
[error] ERROR: Whistle blown! Your build has 2 bobby violation(s) and has been failed! Urgently fix the issues below:
[error]  (1) org.scalatest.scalatest (3.0.0)
[error]      Reason: Example: Required to use latest scalatest 3.1.0+
[error]  (2) uk.gov.hmrc.simple-reactivemongo (7.13.0-play-26)
[error]      Reason: Example: Uses a version of reactivemongo that has a memory leak
[error] stack trace is suppressed; run last Compile / validate for the full output
[error] (Compile / validate) uk.gov.hmrc.bobby.BobbyValidationFailedException: Build failed due to bobby violations. See previous output to resolve
[error] Total time: 1 s, completed 04-Mar-2020 16:46:13
```

The Bobby output consists of a table of all of the dependencies in your build, as well as a summary of any warnings and violations
that are detected.

The table lists violations and warnings at the top (with a level of `ERROR` or `WARN` respectively), and those with no issues after that
with a level of `INFO`. Each row represents one dependency in the build, and where it is pulled in transitively, the actual dependency in your
build that caused it to be pulled in will be shown in the 'Via' column. This is useful as in the case of a transitive violation as it tells you 
what you need to change in order to fix it.

Note that the KEY and colour coding is only applicable when outputting to the console. There is a `bobby-report-<project>-<config>.txt` file generated
with just the table. You can find it in the `target` folder, along with a `.txt` variant which has the content in machine readable form.

## Configuration Options

### Strict mode

You can configure Bobby to be more fussy and fail a build on warnings as well as violations, by turning on strict mode.

To change the strict mode you can:

 * Start SBT with an environment variable, `BOBBY_STRICT_MODE=true sbt validate`
 * Specify it in your build settings
    ```
    bobbyStrictMode := true
    ```
 * Change it manually just for your console session, e.g.
    ```
    set uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.bobbyStrictMode := true
    ```

### Bobby Rule file location
As shown above, you can configure the rules via a setting in `~/.sbt/bobby.conf`. Bobby can read both local or remote files:

```
bobby-rules-url = https://some-url/bobby-rules.json
bobby-rules-url = file:///~/.sbt/bobby-rules.json
```

Alternatively, you can specify the location directly in your `build.sbt`:

First add the required imports:
```
import java.net.URL
import SbtBobbyPlugin.BobbyKeys.bobbyRulesURL
```
Then point to the rule file:
```
bobbyRulesURL := Some(new URL("path to your bobby rules file")),
```

### Changing the output directory

You can override the directory where the reports are written using the setting:

```
outputDirectoryOverride := Some("target/my-dir")
```

Or in the `bobby.conf`:
```
output-directory = target/my-dir
```

### Turning off the console colours

By default, the console output will show with ANSI colours. To turn this off you can:

 * Start SBT with an environment variable, `BOBBY_CONSOLE_COLOURS=false sbt validate`
 * Specify it in your build settings
    ```
    bobbyConsoleColours := false
    ```
 * Change it manually just for your console session, e.g.
    ```
    set uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.bobbyConsoleColours := false
    ```

### Changing the view type

Bobby can display the output table in a few different variations. Currently these are:

* Flat => The standard table, with all columns
* Compact => The standard table, minus the reason column 
* Nested => Shows the local dependencies with their transitives underneath them and indented

To change the view type you can:

 * Start SBT with an environment variable, `BOBBY_VIEW_TYPE=Nested sbt validate`
 * Specify it in your build settings
    ```
    bobbyViewType := Nested
    ```
 * Change it manually just for your console session, e.g.
    ```
    set uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.bobbyViewType := uk.gov.hmrc.bobby.output.Nested
    ```
## How do I change the Bobby rules enforced by Jenkins?

On Jenkins, Bobby sources its config remotely. Current rules can be found at https://github.com/hmrc/bobby-config (available only for people in the HMRC org on github). 

Anyone working on the Tax Platform can add/change bobby rules. We accept pull requests and once merged the new rules will take effect immediately. 

An example commit is as follows. Note that we should always try to stick to one rule per dependency. https://github.com/hmrc/bobby-config/commit/f1b1b180cde857d64e3b1c2fb5322bc400c18c8a

## Developing ##

* The `test-project` folder contains a simple example project which may be useful as a playground:

```
cd test-project
sbt validate
```
 
* Bobby uses [scripted](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html) tests which are executed with ```sbt scripted```

## Notice 

From major version 2, `sbt-bobby` makes use of the [sbt-dependency-graph](https://github.com/jrudolph/sbt-dependency-graph) plugin to compute 
the transitive module graph. In previous versions only the locally declared dependencies were considered.

It also takes some inspiration from [sbt-blockade](https://github.com/Verizon/sbt-blockade)

## License
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

