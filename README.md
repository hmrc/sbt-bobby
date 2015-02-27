#Overview
This is an Sbt plugin that can be used to ensure up-to-date dependencies are being used in an SBT project.

#Background
It can be hard to ensure that distributed teams upgrade to the latest version of a dependency. This is a problem when there are security fixes or other reasons to require a library to be upgraded. Bobby provides the capability to fail builds which have out-of-date dependencies. Ideally communications will be in place to ensure updates happen but Bobby acts as a safety net of last resort.

Currently version 0.2.3 checks your projects' dependency versions against the lastest available.

Bobby will search for your nexus credentials in /.sbt/.credentials.

#How To Use (currently influx)

In your "~/.sbt/0.13/plugins/build.sbt"

set
```
addSbtPlugin("uk.gov.hmrc" % "bobby" % "0.2.3")
```


#### Deprecated Dependencies

It is possible to manually mark certain dependency versions as deprecated. The use of deprecated versions will make the build fail.
This is done via a json file containing the list of all the deprecated libraries
The file name is 'deprecated-dependencies.json'

To tell Bobby where to find this file use ~/.sbt/bobby setting a 'deprecated-dependencies' property

Bobby can read both local or remote files:
```
deprecated-dependencies = https://some-url/bobby-config/deprecated-dependencies.json
```
or
```
deprecated-dependencies = file:///~/.sbt/deprecated-dependencies.json
```


The format of the json is an array of rows like:
```
[
{ "organisation" : "uk.gov.hmrc", "name" : "my-library", "range" : "(,6.0.0)", "reason" : "Versions older than 6.0.0 have a security vulnerability", "from" : "2015-03-15" },
{ "organisation" : "uk.gov.hmrc", "name" : "my-other-library", "range" : "[1.2.0]", "reason" : "1.2.0 has a bug", "from" : "2015-03-15" }
{ "organisation" : "*", "name" : "*", "range" : "[*-SNAPSHOT]", "reason" : "You shouldn't be deploying a snapshot to production should you?", "from" : "2000-01-01" }
]
```

###### Where:
* _organisation_ and _name_ identify the dependency. You can use '*' as wildcard
* _range_ is used to declare minimum, maximum allowed versions of a dependency (both min and max may be optional), and allow "holes" for known incompatible versions. See 'Supported Version Ranges' for more details
* _reason_ tells why the versions in range are deprecated
* _from_ tells when the versions in range become unsupported. The builds will fail after that day. Before only a warning is shown.


##### Supported Version Ranges
| Range  | Meaning  |
|---|---|
| (,1.0.0]  | x <= 1.0.0  |
| [1.0.0]  | Hard requirement on 1.0.0  |
| [1.2.0,1.3.0]  | 1.2.0 <= x <= 1.3.0  |
| [1.0.0,2.0.0)  | 1.0.0 <= x < 2.0.0  |
| [1.5.0,)  | x >= 1.5.0  |
| [*-SNAPSHOT] | Any version with qualifier 'SNAPSHOT' |






