# [sbt-dynver](https://github.com/dwijnand/sbt-dynver)

`sbt-dynver` is an [sbt](http://www.scala-sbt.org/) plugin to dynamically set your version from git.

Inspired by:
* The way that Mercurial [versions itself](https://selenic.com/hg/file/3.9.1/setup.py#l179)
* The [GitVersioning][] AutoPlugin in [sbt-git][].

[sbt]: http://www.scala-sbt.org/
[sbt-git]: https://github.com/sbt/sbt-git
[GitVersioning]: https://github.com/sbt/sbt-git/blob/v0.8.5/src/main/scala/com/typesafe/sbt/SbtGit.scala#L266-L270

## Setup

Add this to your sbt build plugins, in either `project/plugins.sbt` or `project/dynver.sbt`:

    addSbtPlugin("com.dwijnand" % "sbt-dynver" % <latest-release>)

Find the latest release from the [Releases tab](https://github.com/dwijnand/sbt-dynver/releases) in GitHub.

Then make sure to **NOT set the version setting**, otherwise you will override `sbt-dynver`.

Other than that, as `sbt-dynver` is an AutoPlugin that is all that is required.

## Detail

`version in ThisBuild` will be automatically set to:

```
* 1.0.0                          (if building on v1.0.0 tag, w/o local changes)
* 1.0.0+20140707-1030            (if building on v1.0.0 tag with local changes)
* 1.0.0+3-1234abcd               (if building on commit 1234abcd: 3 commits after v1.0.0 tag, w/o local changes)
* 1.0.0+3-1234abcd+20140707-1030 (if building on commit 1234abcd: 3 commits after v1.0.0 tag with local changes)
* 1234abcd                       (if building & there are no tags, on commit 1234abcd, w/o local changes)
* 1234abcd+20140707-1030         (if building & there are no tags, on commit 1234abcd with local changes)
* HEAD+20140707-1030             (if building & there are no commits, or the project isn't a git repo)
```

`isSnapshot in ThisBuild` will be automatically set to:

```
* false (if building on v1.0.0 tag, w/o local changes)
* true  (if building on v1.0.0 tag with local changes)
* false (if building on commit 1234abcd: 3 commits after v1.0.0 tag, w/o local changes)
* true  (if building on commit 1234abcd: 3 commits after v1.0.0 tag with local changes)
* true  (if building & there are no tags, on commit 1234abcd, w/o local changes)
* true  (if building & there are no tags, on commit 1234abcd with local changes)
* true  (if building & there are no commits, or the project isn't a git repo)
```

## Tasks

* `dynver`: Returns the version of your project, from git
* `dynverCheckVersion`: Checks if version and dynver match
* `dynverAssertVersion`: Asserts if version and dynver match

## Dependencies

* `git`, on the `PATH`

## Licence

Copyright 2016 Dale Wijnand

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
