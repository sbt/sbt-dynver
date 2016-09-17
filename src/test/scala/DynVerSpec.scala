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
    val git = newRepo()

    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    version(git) ?= "1.0.0"
  }

  def tagDirty(): Prop = {
    val git = newRepo()

    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    version(git) ?= "1.0.0+20160917"
  }

  def tagChangesClean(): Prop = {
    val git = newRepo()

    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("2").call()

    val sha = commit.abbreviate(8).name()

    version(git) ?= s"1.0.0+1-$sha"
  }

  def tagChangesDirty(): Prop = {
    val git = newRepo()

    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("2").call()

    val sha = commit.abbreviate(8).name()

    Files.write(file, Seq("3").asJava, CREATE, APPEND)

    version(git) ?= s"1.0.0+1-$sha+20160917"
  }

  def noTagsClean(): Prop = {
    val git = newRepo()

    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("1").call()

    val sha = commit.abbreviate(8).name()

    version(git) ?= sha
  }

  def noTagsDirty(): Prop = {
    val git = newRepo()

    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    val commit = git.commit().setMessage("1").call()

    val sha = commit.abbreviate(8).name()

    Files.write(file, Seq("2").asJava, CREATE, APPEND)

    version(git) ?= s"$sha+20160917"
  }

  def noCommits(): Prop = version(newRepo())            ?= "HEAD+20160917"
  def noGitRepo(): Prop = versionAtDir(createTempDir()) ?= "HEAD+20160917"


  private def newRepo() = Git.init().setDirectory(createTempDir()).call()

  private def createTempDir() = {
    val dir = Files.createTempDirectory(s"dynver-test-").toFile
    dir.deleteOnExit()
    dir
  }

  private val fakeClock = FakeClock(new GregorianCalendar(2016, 9, 17).getTime)

  private def version(git: Git) = versionAtDir(git.getRepository.getWorkTree)

  private def versionAtDir(dir: File) = DynVer(Some(dir), fakeClock).version()
}
