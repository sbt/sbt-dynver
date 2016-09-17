import java.nio.file._, StandardOpenOption._
import java.io._
import java.util.{ Properties => _, _ }

import scala.collection.JavaConverters._

import org.scalacheck._, Prop._
import org.eclipse.jgit.api._
import sbtdynver._

object DynVerSpec extends Properties("DynVerSpec") {
  property("not a git repo") = noGitRepo()
  property("no commits") = noCommits()
  property("on commit, w/o local changes") = noTagsClean()
  property("on commit with local changes") = noTagsDirty()
  property("on tag v1.0.0, w/o local changes") = tagClean()
  property("on tag v1.0.0 with local changes") = tagDirty()
  property("on 1 commit after v1.0.0 tag, w/o local changes") = tagChangesClean()
  property("on 1 commit after v1.0.0 tag with local changes") = tagChangesDirty()

  def noGitRepo(): Prop = versionAtDir(createTempDir()) ?= "HEAD+20160917"
  def noCommits(): Prop = version(newRepo())            ?= "HEAD+20160917"

  def noTagsClean(): Prop = {
    val git = newRepo()
    writeToFile(git)
    val sha = commit(git)
    version(git) ?= sha
  }

  def noTagsDirty(): Prop = {
    val git = newRepo()
    writeToFile(git)
    val sha = commit(git)
    writeToFile(git)
    version(git) ?= s"$sha+20160917"
  }

  def tagClean(): Prop = {
    val git = newRepo()
    writeToFile(git)
    commit(git)
    tag(git)
    version(git) ?= "1.0.0"
  }

  def tagDirty(): Prop = {
    val git = newRepo()
    writeToFile(git)
    commit(git)
    tag(git)
    writeToFile(git)
    version(git) ?= "1.0.0+20160917"
  }

  def tagChangesClean(): Prop = {
    val git = newRepo()
    writeToFile(git)
    commit(git)
    tag(git)
    writeToFile(git)
    val sha = commit(git)
    version(git) ?= s"1.0.0+1-$sha"
  }

  def tagChangesDirty(): Prop = {
    val git = newRepo()
    writeToFile(git)
    commit(git)
    tag(git)
    writeToFile(git)
    val sha = commit(git)
    writeToFile(git)
    version(git) ?= s"1.0.0+1-$sha+20160917"
  }


  private def newRepo() = Git.init().setDirectory(createTempDir()).call()

  def writeToFile(git: Git) = {
    val file = git.getRepository.getWorkTree.toPath.resolve("f.txt")
    Files.write(file, Seq("1").asJava, CREATE, APPEND)
  }

  private def commit(git: Git) = {
    git.add().addFilepattern(".").call()
    val commit = git.commit().setMessage("1").call()
    commit.abbreviate(8).name()
  }

  def tag(git: Git) =  git.tag().setName("v1.0.0").setAnnotated(true).call()

  private def version(git: Git) = versionAtDir(git.getRepository.getWorkTree)

  private def versionAtDir(dir: File) = DynVer(Some(dir), fakeClock).version()

  private def createTempDir() = {
    val dir = Files.createTempDirectory(s"dynver-test-").toFile
    dir.deleteOnExit()
    dir
  }

  private val fakeClock = FakeClock(new GregorianCalendar(2016, 9, 17).getTime)
}
