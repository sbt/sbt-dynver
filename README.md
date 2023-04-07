# sbt-dynver

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

    addSbtPlugin("com.github.sbt" % "sbt-dynver" % "x.y.z")
    // Until version 4.1.1:
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

The latest release is: [![release-badge][]][release]

Then make sure to **NOT set the version setting**, otherwise you will override `sbt-dynver`.

In CI, you may need to run `git fetch --unshallow` (or, sometimes, `git fetch --depth=10000`), to avoid
situations where a shallow clone will result in the last tag not being fetched.  Additionally `git fetch --tags`
if the repo is cloned with `--no-tags`.

Other than that, as `sbt-dynver` is an AutoPlugin that is all that is required.

## Detail

`ThisBuild / version`, `ThisBuild / isSnapshot` and `ThisBuild / isVersionStable` will be automatically set to:

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
mimaPreviousArtifacts := previousStableVersion.value.map(organization.value %% moduleName.value % _).toSet
```

## Tag Requirements

In order to be recognized by sbt-dynver, by default tags must begin with the lowercase letter 'v' followed by a digit.

If you're not seeing what you expect, then either start with this:

    git tag -a v0.0.1 -m "Initial version tag for sbt-dynver"

or change the value of `ThisBuild / dynverVTagPrefix` to remove the requirement for the v-prefix:

    ThisBuild / dynverVTagPrefix := false

or, more generally, use `ThisBuild / dynverTagPrefix` to fully customising tag prefixes, for example:

    ThisBuild / dynverTagPrefix := "foo-" // our tags have the format foo-<version>, e.g. foo-1.2.3

## Tasks

* `dynver`: Returns the dynamic version of your project, inferred from the git metadata
* `dynverCurrentDate`: Returns the captured current date. Used for (a) the dirty suffix of `dynverGitDescribeOutput` and (b) the fallback version (e.g if not a git repo).
* `dynverGitDescribeOutput`: Returns the captured `git describe` out, in a structured form. Useful to define a [custom version string](#custom-version-string).
* `dynverCheckVersion`: Checks if version and dynver match
* `dynverAssertVersion`: Asserts if version and dynver match

## Publishing to Sonatype's snapshots repository (aka "Sonatype mode")

If you're publishing to Sonatype sonashots then enable `ThisBuild / dynverSonatypeSnapshots := true` to append
"-SNAPSHOT" to the version if `isSnapshot` is `true` (which it is unless building on a tag with no local
changes).  This opt-in exists because the Sonatype's snapshots repository requires all versions to end with
`-SNAPSHOT`.

## Portable version strings

The default version string format includes `+` characters, which is an escape character in URL and is not compatible with docker tags.
This character can be overridden by setting:

```scala
ThisBuild / dynverSeparator := "-"
```

## Custom version string

Sometimes you want to customise the version string. It might be for personal preference, or for compatibility with another tool or spec.

For simple cases you can customise a version by simply post-processing the value of `ThisBuild / version` (and optionally `ThisBuild / dynver`), for example by replacing '+' with '-' (emulating the docker support mentioned above):

```scala
ThisBuild / version ~= (_.replace('+', '-'))
ThisBuild / dynver  ~= (_.replace('+', '-'))
```

To completely customise the string format you can use `dynverGitDescribeOutput`, `dynverCurrentDate` and `sbtdynver.DynVer`, like so:

```scala
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val dirtySuffix = out.dirtySuffix.dropPlus.mkString("-", "")
  if (out.isCleanAfterTag) out.ref.dropPrefix + dirtySuffix // no commit info if clean after tag
  else out.ref.dropPrefix + out.commitSuffix.mkString("-", "-", "") + dirtySuffix
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

Or, using sbt-dynver v1.1.0 to v4.0.0:

```scala
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    throw new MessageOnlyException(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
    )
  s
}
```


## Dependencies

* `git`, on the `PATH`

## FAQ

### How do I make previousStableVersion return None for major version branches?

Deciding whether going from one version to another is a "breaking change" is out of scope for this project.
If you have binary compatibility check setup using `previousStableVersion` in CI
and want to skip the check for major version branches (e.g. `1.x` vs `2.x`), see https://github.com/sbt/sbt-dynver/issues/70#issuecomment-458620722
for the recommended solution.
