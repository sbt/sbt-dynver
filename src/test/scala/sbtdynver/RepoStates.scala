package sbtdynver

import java.nio.file._
import StandardOpenOption._
import java.util._

import org.eclipse.jgit.api.MergeCommand.FastForwardMode

import scala.collection.JavaConverters._
import org.eclipse.jgit.api._
import org.eclipse.jgit.merge.MergeStrategy

object RepoStates {
  def notAGitRepo()                     = State()
  def noCommits()                       = notAGitRepo().init()
  def onCommit()                        = noCommits().commit()
  def onCommitDirty()                   = onCommit().dirty()
  def onTag(n: String = "v1.0.0")       = onCommit().tag(n)
  def onTagDirty()                      = onTag().dirty()
  def onTagAndCommit()                  = onTag().commit()
  def onTagAndCommitDirty()             = onTagAndCommit().dirty()
  def onTagAndSecondCommit()            = onTagAndCommitDirty().commit()
  def onSecondTag(n: String = "v2.0.0") = onTagAndSecondCommit().tag(n)

  final case class State() {
    val dir = doto(Files.createTempDirectory(s"dynver-test-").toFile)(_.deleteOnExit())
    val date = new GregorianCalendar(2016, 8, 17).getTime
    val dynver = DynVer(Some(dir))

    var git: Git = _
    var sha: String = "undefined"

    def init()         = andThis(git = Git.init().setDirectory(dir).call())
    def dirty()        = {
      // We randomize the content added otherwise we will get the same git hash for two separate commits
      // because our commits are made at almost the same time
      andThis(Files.write(dir.toPath.resolve("f.txt"), Seq(scala.util.Random.nextString(10)).asJava, CREATE, APPEND))
    }
    def tag(n: String) = andThis(git.tag().setName(n).call())

    def commit() = andThis {
      dirty()
      git.add().addFilepattern(".").call()
      sha = git.commit().setMessage("1").call().abbreviate(8).name()
    }

    def branch(branchName: String) = andThis {
      git.branchCreate().setName(branchName).call()
      git.checkout().setName(branchName).call()
    }

    def checkout(branchName: String) = andThis {
      git.checkout().setName(branchName).call()
      sha = getCurrentHeadSHA
    }

    def merge(branchName: String) = andThis {
      git.merge()
        .include(git.getRepository.findRef(branchName))
        .setCommit(true)
        .setFastForward(FastForwardMode.NO_FF)
        .setStrategy(MergeStrategy.OURS)
        .call()
      sha = getCurrentHeadSHA
    }

    def version()         = dynver.version(date).replaceAllLiterally(sha, "1234abcd")
    def sonatypeVersion() = dynver.sonatypeVersion(date).replaceAllLiterally(sha, "1234abcd")
    def previousVersion() = dynver.previousVersion
    def isSnapshot()      = dynver.isSnapshot()
    def isVersionStable() = dynver.isVersionStable()

    private def doalso[A, U](x: A)(xs: U*)  = x
    private def doto[A, U](x: A)(f: A => U) = doalso(x)(f(x))
    private def andThis[U](x: U)            = this

    private def getCurrentHeadSHA = {
      git.getRepository.findRef("HEAD").getObjectId.abbreviate(8).name()
    }
  }

}
