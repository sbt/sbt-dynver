# [sbt-dynver][] [![travis-badge][]][travis] [![gitter-badge][]][gitter] [![release-badge][]][release]

[sbt-dynver]:                       https://github.com/dwijnand/sbt-dynver
[travis]:                        https://travis-ci.org/dwijnand/sbt-dynver
[travis-badge]:                  https://travis-ci.org/dwijnand/sbt-dynver.svg?branch=master
[gitter]:                            https://gitter.im/dwijnand/sbt-dynver
[gitter-badge]:               https://badges.gitter.im/dwijnand/sbt-dynver.svg
[release]:                          https://github.com/dwijnand/sbt-dynver/releases/latest
[release-badge]: https://img.shields.io/github/release/dwijnand/sbt-dynver.svg

`sbt-dynver` is an [sbt](http://www.scala-sbt.org/) plugin to dynamically set your version from git.

Inspired by:
* The way that Mercurial [versions itself](https://selenic.com/hg/file/3.9.1/setup.py#l179)
* The [GitVersioning][] AutoPlugin in [sbt-git][].

Features:
* Dynamically set your version by looking at the closest tag to the current commit
* Detect the previous version
    * Useful for automatic [binary compatibility checks](https://github.com/lightbend/migration-manager) across library versions

[sbt-git]: https://github.com/sbt/sbt-git
[GitVersioning]: https://github.com/sbt/sbt-git/blob/v0.8.5/src/main/scala/com/typesafe/sbt/SbtGit.scala#L266-L270

## Setup

Add this to your sbt build plugins, in either `project/plugins.sbt` or `project/dynver.sbt`:

    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "x.y.z")

The latest release is: [![release-badge][]][release]

Then make sure to **NOT set the version setting**, otherwise you will override `sbt-dynver`.

In CI, you may need to run `git fetch --tags` if the repo is cloned with `--no-tags`.

Other than that, as `sbt-dynver` is an AutoPlugin that is all that is required.

## Detail

`version in ThisBuild`, `isSnapshot in ThisBuild` and `isVersionStable in ThisBuild` will be automatically set to:

```
| tag    | dist | HEAD sha | dirty | version                        | isSnapshot | isVersionStable |
| ------ | ---- | -------- | ----- | ------------------------------ | ---------- | --------------- |
| v1.0.0 | 0    | -        | No    | 1.0.0                          | false      | true            |
| v1.0.0 | 0    | 1234abcd | Yes   | 1.0.0+0-1234abcd+20140707-1030 | true       | false           |
| v1.0.0 | 3    | 1234abcd | No    | 1.0.0+3-1234abcd               | true       | true            |
| v1.0.0 | 3    | 1234abcd | Yes   | 1.0.0+3-1234abcd+20140707-1030 | true       | false           |
| <none> | 3    | 1234abcd | No    | 0.0.0+3-1234abcd               | true       | true            |
| <none> | 3    | 1234abcd | Yes   | 0.0.0+3-1234abcd+20140707-1030 | true       | false           |
| no commits or no git repo at all | HEAD+20140707-1030             | true       | false           |
```

Where:
* `tag` means what is the latest tag (relative to HEAD)
* `dist` means the distance of the HEAD commit from the tag
* `dirty` refers to whether there are local changes in the git repo

#### Previous Version Detection

Given the following git history, here's what `previousStableVersion` returns when at each commit:

```
*   (tagged: v1.1.0)       --> Some("1.0.0")
*   (untagged)             --> Some("1.0.0")
| * (tagged: v2.1.0)       --> Some("2.0.0")
| * (tagged: v2.0.0)       --> Some("1.0.0")
|/  
*   (tagged: v1.0.0)       --> None
*   (untagged)             --> None
```

Previous version is detected by looking at the closest tag of the parent commit of HEAD.

If the current commit has multiple parents, the first parent will be used. In git, the first parent
comes from the branch you merged into (e.g. `master` in `git checkout master && git merge my-feature-branch`)

To use this feature with the Migration Manager [MiMa](https://github.com/lightbend/migration-manager) sbt plugin, add

```scala
mimaPreviousArtifacts := previousStableVersion.value.map(organization.value %% name.value % _).toSet
```

## Tag Requirements

In order to be recognized by sbt-dynver, by default tags must begin with the lowercase letter 'v' followed by a digit.

If you're not seeing what you expect, then either start with this:

    git tag -a v0.0.1 -m "Initial version tag for sbt-dynver"

or change the value of `dynverVTagPrefix in ThisBuild` to remove the requirement for the v-prefix:

    dynverVTagPrefix in ThisBuild := false

## Tasks

* `dynver`: Returns the dynamic version of your project, inferred from the git metadata
* `dynverCurrentDate`: Returns the captured current date. Used for (a) the dirty suffix of `dynverGitDescribeOutput` and (b) the fallback version (e.g if not a git repo).
* `dynverGitDescribeOutput`: Returns the captured `git describe` out, in a structured form. Useful to define a [custom version string](#custom-version-string).
* `dynverCheckVersion`: Checks if version and dynver match
* `dynverAssertVersion`: Asserts if version and dynver match

## Publishing to Sonatype's snapshots repository (aka "Sonatype mode")

If you're publishing to Sonatype sonashots then enable `dynverSonatypeSnapshots in ThisBuild := true` to append
"-SNAPSHOT" to the version if `isSnapshot` is `true` (which it is unless building on a tag with no local
changes).  This opt-in exists because the Sonatype's snapshots repository requires all versions to end with
`-SNAPSHOT`.

## Docker-compatible version strings

The default version string format includes `+` characters, which is not compatible with docker tags. This character can be overridden by setting:

```scala
dynverSeparator in ThisBuild := "-"
```

## Custom version string

Sometimes you want to customise the version string. It might be for personal preference, or for compatibility with another tool or spec.

For simple cases you can customise a versiou by simply post-processing the value of `version in ThisBuild` (and optionally `dynver in ThisBuild`), for example by replacing '+' with '-' (emulating the docker support mentioned above):

```scala
version in ThisBuild ~= (_.replace('+', '-'))
 dynver in ThisBuild ~= (_.replace('+', '-'))
```

To completely customise the string format you can use `dynverGitDescribeOutput`, `dynverCurrentDate` and `sbtdynver.DynVer`, like so:

```scala
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val dirtySuffix = out.dirtySuffix.dropPlus.mkString("-", "")
  if (out.isCleanAfterTag) out.ref.dropV.value + dirtySuffix // no commit info if clean after tag
  else out.ref.dropV.value + out.commitSuffix.mkString("-", "-", "") + dirtySuffix
}

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer timestamp d}"

inThisBuild(List(
  version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value)),
   dynver := {
     val d = new java.util.Date
     sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
   }
))
```

Essentially this:

1. defines how to transform the structured output of `git describe`'s into a string, with `versionFmt`
2. defines the fallback version string, with `fallbackVersion`, and
3. wires everything back together

## Sanity checking the version

As a sanity check, you can stop the build from loading by running a check during sbt's `onLoad`.
For instance, to make sure that the version is derived from tags you can use:

```scala
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}
```

This will return an error message like the following:

```
[error] Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: 3-d9489763
```

## Dependencies

* `git`, on the `PATH`

## FAQ

### How do I make previousStableVersion return None for major version branches?

Deciding whether going from one version to another is a "breaking change" is out of scope for this project. 
If you have binary compatibility check setup using `previousStableVersion` in CI
and want to skip the check for major version branches (e.g. `1.x` vs `2.x`), see https://github.com/dwijnand/sbt-dynver/issues/70#issuecomment-458620722 
for the recommended solution.

## Licence

Copyright 2016-2017 Dale Wijnand

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
