package sbtdynver

import java.nio.file._
import StandardOpenOption._
import java.util._

import org.eclipse.jgit.api.MergeCommand.FastForwardMode

import scala.collection.JavaConverters._
import org.eclipse.jgit.api._
import org.eclipse.jgit.merge.MergeStrategy

object RepoStates extends RepoStates(tagPrefix = "v")

sealed class RepoStates(tagPrefix: String) {
  def notAGitRepo()                     = new State()
  def noCommits()                       = notAGitRepo().init()
  def onCommit()                        = noCommits().commit().commit().commit()
  def onCommitDirty()                   = onCommit().dirty()
  def onTag(n: String = "1.0.0")        = onCommit().tag(optPrefix(n))
  def onTagDirty()                      = onTag().dirty()
  def onTagAndCommit()                  = onTag().commit()
  def onTagAndCommitDirty()             = onTagAndCommit().dirty()
  def onTagAndSecondCommit()            = onTagAndCommitDirty().commit()
  def onSecondTag(n: String = "2.0.0")  = onTagAndSecondCommit().tag(optPrefix(n))

  private def optPrefix(s: String) = if (s.startsWith(tagPrefix)) s else s"$tagPrefix$s"

  locally {
    JGitSystemReader.init // see JGitSystemReader's docs
    noCommits() // & seed JGit's FS.FileStoreAttributes.attributeCache with my tmp directory's BsdFileStore
  }

  final class State() {
    val dir = doto(Files.createTempDirectory(s"dynver-test-").toFile)(_.deleteOnExit())
    val date = new GregorianCalendar(2016, 8, 17).getTime
    val dynver = DynVer(Some(dir), DynVer.separator, tagPrefix)

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
      sha = git.commit().setMessage("1").setSign(false).call().abbreviate(8).name()
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
        .setCommit(false)
        .setFastForward(FastForwardMode.NO_FF)
        .setStrategy(MergeStrategy.OURS)
        .call()
      commit()
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
