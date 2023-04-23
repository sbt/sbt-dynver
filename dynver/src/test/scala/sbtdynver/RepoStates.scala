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
  def notAGitRepo   = new State
  def noCommits     = notAGitRepo.init
  def onCommit      = noCommits.commit.commit.commit
  def onTag         = onCommit.tag("1.0.0")
  def onBranch2x    = onTag.branch("2.x").commit
  def onBranch2Tag  = onBranch2x.tag("2.0.0")
  def onMultiBranch = onBranch2Tag.commit.tag("2.1.0").checkout("master").commit.tag("1.1.0")

  private def optPrefix(s: String) = if (s.startsWith(tagPrefix)) s else s"$tagPrefix$s"

  // seed JGit's FS.FileStoreAttributes.attributeCache with my tmp directory's BsdFileStore
  locally(noCommits)

  final class State() {
    val dir    = doto(Files.createTempDirectory(s"dynver-test-").toFile)(_.deleteOnExit)
    val sep    = DynVer.separator
    val date   = new GregorianCalendar(2016, 8, 17).getTime
    val dynver = DynVer(Some(dir), sep, tagPrefix)

    var git: Git = _
    var sha      = "undefined"

    def init         = andThis { git = Git.init.setDirectory(dir).call() }
    def dirty        = {
      // We randomize the content added otherwise we will get the same git hash for two separate commits
      // because our commits are made at almost the same time
      andThis(Files.write(dir.toPath.resolve("f.txt"), Seq(scala.util.Random.nextString(10)).asJava, CREATE, APPEND))
    }
    def tag(n: String) = andThis(git.tag.setName(optPrefix(n)).call())

    def commit = andThis {
      dirty
      git.add.addFilepattern(".").call()
      sha = git.commit.setMessage("1").setSign(false).call().abbreviate(8).name
    }

    def branch(branchName: String) = andThis {
      git.branchCreate.setName(branchName).call()
      git.checkout.setName(branchName).call()
    }

    def checkout(branchName: String) = andThis {
      git.checkout.setName(branchName).call()
      sha = getHeadSHA
    }

    def merge(branchName: String) = andThis {
      git.merge
        .include(git.getRepository.findRef(branchName))
        .setCommit(false)
        .setFastForward(FastForwardMode.NO_FF)
        .setStrategy(MergeStrategy.OURS)
        .call()
      commit
      sha = getHeadSHA
    }

    def version         = dynver.        version(date).replaceAllLiterally(sha, "1234abcd")
    def sonatypeVersion = dynver.sonatypeVersion(date).replaceAllLiterally(sha, "1234abcd")
    def previousVersion = dynver.previousVersion
    def isSnapshot      = dynver.isSnapshot()
    def isVersionStable = dynver.isVersionStable()

    private def doalso[A, U](x: A)(xs: U*)  = x
    private def doto[A, U](x: A)(f: A => U) = doalso(x)(f(x))
    private def andThis[U](x: U)            = this

    private def getHeadSHA = git.getRepository.findRef("HEAD").getObjectId.abbreviate(8).name
  }
}
