import java.nio.file._, StandardOpenOption._
import java.io._
import java.util.{ Properties => _, _ }

import scala.collection.JavaConverters._

import org.scalacheck._, Prop._
import org.eclipse.jgit.api._
import sbtdynver._

object DynVerSpec extends Properties("DynVerSpec") {
  property("when on v1.0.0 tag, w/o local changes") = tagClean()
  property("when on v1.0.0 tag with local changes") = tagDirty()
  property("when on commit 1234abcd: 3 commits after v1.0.0 tag, w/o local changes") = tagChangesClean()
  property("when on commit 1234abcd: 3 commits after v1.0.0 tag with local changes") = tagChangesDirty()
  property("when there are no tags, on commit 1234abcd, w/o local changes") = noTagsClean()
  property("when there are no tags, on commit 1234abcd with local changes") = noTagsDirty()
  property("when there are no commits") = noCommits()
  property("when not a git repo") = noGitRepo()

  def tagClean(): Prop = {
    val dir = createTempDir("tag-clean")

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    val dynVer = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynVer.version() ?= "1.0.0"
  }

  def tagDirty(): Prop = {
    val dir = createTempDir("tag-dirty")

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= "1.0.0+20160917"
  }

  def tagChangesClean(): Prop = {
    val dir = createTempDir("tag-changes-clean")

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("2").call()

    val sha = commit.abbreviate(8).name()

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= s"1.0.0+1-$sha"
  }

  def tagChangesDirty(): Prop = {
    val dir = createTempDir("tag-changes-dirty")

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("2").call()

    val sha = commit.abbreviate(8).name()

    Files.write(file, Seq("3").asJava, CREATE, APPEND)

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= s"1.0.0+1-$sha+20160917"
  }

  def noTagsClean(): Prop = {
    val dir = createTempDir("no-tags-clean")

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("1").call()

    val sha = commit.abbreviate(8).name()

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= sha
  }

  def noTagsDirty(): Prop = {
    val dir = createTempDir("no-tags-dirty")

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("1").call()

    val sha = commit.abbreviate(8).name()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= s"$sha+20160917"
  }

  def noCommits(): Prop = {
    val dir = createTempDir("no-commits")

    val git = Git.init().setDirectory(dir).call()

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= "HEAD+20160917"
  }

  def noGitRepo(): Prop = {
    val dir = createTempDir("no-git-repo")

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= "HEAD+20160917"
  }

  def createTempDir(id: String): File = {
    val dir = Files.createTempDirectory(s"dynver-test-$id-").toFile
    dir.deleteOnExit()
    dir
  }
}
