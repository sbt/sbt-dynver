import java.nio.file._, StandardOpenOption._
import java.util.{ Properties => _, _ }

import scala.collection.JavaConverters._

import org.scalacheck._, Prop._
import org.eclipse.jgit.api._
import sbtdynver._
import RepoStates._

object VersionSpec extends Properties("VersionSpec") {
  property("not a git repo")                                = notAGitRepo().version()         ?= "HEAD+20160917"
  property("no commits")                                    = noCommits().version()           ?= "HEAD+20160917"
  property("on commit, w/o local changes")                  = onCommit().version()            ?= "1234abcd"
  property("on commit with local changes")                  = onCommitDirty().version()       ?= "1234abcd+20160917"
  property("on tag v1.0.0, w/o local changes")              = onTag().version()               ?= "1.0.0"
  property("on tag v1.0.0 with local changes")              = onTagDirty().version()          ?= "1.0.0+20160917"
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTagAndCommit().version()      ?= "1.0.0+1-1234abcd"
  property("on tag v1.0.0 and 1 commit with local changes") = onTagAndCommitDirty().version() ?= "1.0.0+1-1234abcd+20160917"
}

object IsSnapshotSpec extends Properties("IsSnapshotSpec") {
//  property("not a git repo")                                = notAGitRepo().isSnapshot()         ?= true
  property("no commits")                                    = noCommits().isSnapshot()           ?= true
  property("on commit, w/o local changes")                  = onCommit().isSnapshot()            ?= true
  property("on commit with local changes")                  = onCommitDirty().isSnapshot()       ?= true
  property("on tag v1.0.0, w/o local changes")              = onTag().isSnapshot()               ?= false
  property("on tag v1.0.0 with local changes")              = onTagDirty().isSnapshot()          ?= true
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTagAndCommit().isSnapshot()      ?= false
  property("on tag v1.0.0 and 1 commit with local changes") = onTagAndCommitDirty().isSnapshot() ?= true
}

object RepoStates {
  def notAGitRepo()         = State()
  def noCommits()           = notAGitRepo().init()
  def onCommit()            = noCommits().commit()
  def onCommitDirty()       = onCommit().dirty()
  def onTag()               = onCommit().tag()
  def onTagDirty()          = onTag().dirty()
  def onTagAndCommit()      = onTag().commit()
  def onTagAndCommitDirty() = onTagAndCommit().dirty()

  final case class State() {
    val dir = doto(Files.createTempDirectory(s"dynver-test-").toFile)(_.deleteOnExit())
    val fakeClock = FakeClock(new GregorianCalendar(2016, 9, 17).getTime)
    val dynver = DynVer(Some(dir), fakeClock)

    var git: Git = _
    var sha: String = "undefined"

    def init()  = andThis(git = Git.init().setDirectory(dir).call())
    def dirty() = andThis(Files.write(dir.toPath.resolve("f.txt"), Seq("1").asJava, CREATE, APPEND))
    def tag()   = andThis(git.tag().setName("v1.0.0").setAnnotated(true).call())

    def commit() = andThis {
      dirty()
      git.add().addFilepattern(".").call()
      sha = git.commit().setMessage("1").call().abbreviate(8).name()
    }

    def version()    = dynver.version().replaceAllLiterally(sha, "1234abcd")
    def isSnapshot() = dynver.isSnapshot()

    private def doalso[A, U](x: A)(xs: U*)  = x
    private def doto[A, U](x: A)(f: A => U) = doalso(x)(f(x))
    private def andThis[U](x: U)            = this
  }
}
