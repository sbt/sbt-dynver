import java.nio.file._, StandardOpenOption._
import java.util.{ Properties => _, _ }

import scala.collection.JavaConverters._

import org.scalacheck._, Prop._
import org.eclipse.jgit.api._
import sbtdynver._

object DynVerSpec extends Properties("DynVerSpec") {
  property("not a git repo") = notAGitRepo()
  property("no commits") = noCommits()
  property("on commit, w/o local changes") = onCommit()
  property("on commit with local changes") = onCommitDirty()
  property("on tag v1.0.0, w/o local changes") = onTag()
  property("on tag v1.0.0 with local changes") = onTagDirty()
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTagAndCommit()
  property("on tag v1.0.0 and 1 commit with local changes") = onTagAndCommitDirty()

  def notAGitRepo(): Prop = State().version()        ?= "HEAD+20160917"
  def noCommits(): Prop   = State().init().version() ?= "HEAD+20160917"

  def onCommit(): Prop = State().init().commit().version() ?= "1234abcd"

  def onCommitDirty(): Prop = State().init().commit().dirty().version() ?= "1234abcd+20160917"

  def onTag(): Prop = State().init().commit().tag().version() ?= "1.0.0"

  def onTagDirty(): Prop = State().init().commit().tag().dirty().version() ?= "1.0.0+20160917"

  def onTagAndCommit(): Prop = State().init().commit().tag().commit().version() ?= "1.0.0+1-1234abcd"

  def onTagAndCommitDirty(): Prop = State().init().commit().tag().commit().dirty().version() ?= "1.0.0+1-1234abcd+20160917"

  final case class State() {
    val dir = Files.createTempDirectory(s"dynver-test-").toFile
    dir.deleteOnExit()

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

    def version() = dynver.version().replaceAllLiterally(sha, "1234abcd")

    private def andThis[U](x: U): this.type = this
  }
}
