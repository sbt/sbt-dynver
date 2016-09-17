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
  property("when on commit 1234abcd: 1 commits after v1.0.0 tag, w/o local changes") = tagChangesClean()
  property("when on commit 1234abcd: 1 commits after v1.0.0 tag with local changes") = tagChangesDirty()
  property("when there are no tags, on commit 1234abcd, w/o local changes") = noTagsClean()
  property("when there are no tags, on commit 1234abcd with local changes") = noTagsDirty()
  property("when there are no commits") = noCommits()
  property("when not a git repo") = noGitRepo()

  def tagClean(): Prop = {
    val dir = createTempDir()

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    version(dir) ?= "1.0.0"
  }

  def tagDirty(): Prop = {
    val dir = createTempDir()

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    version(dir) ?= "1.0.0+20160917"
  }

  def tagChangesClean(): Prop = {
    val dir = createTempDir()

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

    version(dir) ?= s"1.0.0+1-$sha"
  }

  def tagChangesDirty(): Prop = {
    val dir = createTempDir()

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

    version(dir) ?= s"1.0.0+1-$sha+20160917"
  }

  def noTagsClean(): Prop = {
    val dir = createTempDir()

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("1").call()

    val sha = commit.abbreviate(8).name()

    version(dir) ?= sha
  }

  def noTagsDirty(): Prop = {
    val dir = createTempDir()

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("1").call()

    val sha = commit.abbreviate(8).name()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    version(dir) ?= s"$sha+20160917"
  }

  def noCommits(): Prop = {
    val dir = createTempDir()

    Git.init().setDirectory(dir).call()

    version(dir) ?= "HEAD+20160917"
  }

  def noGitRepo(): Prop = {
    val dir = createTempDir()

    version(dir) ?= "HEAD+20160917"
  }

  private def createTempDir() = {
    val dir = Files.createTempDirectory(s"dynver-test-").toFile
    dir.deleteOnExit()
    dir
  }

  private val fakeClock = FakeClock(new GregorianCalendar(2016, 9, 17).getTime)

  private def version(dir: File): String = DynVer(Some(dir), fakeClock).version()
}
