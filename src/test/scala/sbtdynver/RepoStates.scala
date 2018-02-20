package sbtdynver

import java.nio.file._, StandardOpenOption._
import java.util._

import scala.collection.JavaConverters._

import org.eclipse.jgit.api._

object RepoStates {
  def notAGitRepo()               = State()
  def noCommits()                 = notAGitRepo().init()
  def onCommit()                  = noCommits().commit()
  def onCommitDirty()             = onCommit().dirty()
  def onTag(n: String = "v1.0.0") = onCommit().tag(n)
  def onTagDirty()                = onTag().dirty()
  def onTagAndCommit()            = onTag().commit()
  def onTagAndCommitDirty()       = onTagAndCommit().dirty()

  final case class State() {
    val dir = doto(Files.createTempDirectory(s"dynver-test-").toFile)(_.deleteOnExit())
    val date = new GregorianCalendar(2016, 8, 17).getTime
    val dynver = DynVer(Some(dir),"")

    var git: Git = _
    var sha: String = "undefined"

    def init()         = andThis(git = Git.init().setDirectory(dir).call())
    def dirty()        = andThis(Files.write(dir.toPath.resolve("f.txt"), Seq("1").asJava, CREATE, APPEND))
    def tag(n: String) = andThis(git.tag().setName(n).call())

    def commit() = andThis {
      dirty()
      git.add().addFilepattern(".").call()
      sha = git.commit().setMessage("1").call().abbreviate(8).name()
    }

    def version()         = dynver.version(date).replaceAllLiterally(sha, "1234abcd")
    def isSnapshot()      = dynver.isSnapshot()
    def isVersionStable() = dynver.isVersionStable()

    private def doalso[A, U](x: A)(xs: U*)  = x
    private def doto[A, U](x: A)(f: A => U) = doalso(x)(f(x))
    private def andThis[U](x: U)            = this
  }
}
