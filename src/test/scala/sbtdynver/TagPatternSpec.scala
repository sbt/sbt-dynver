package sbtdynver

import org.scalacheck._, Prop._, util.Pretty, Pretty.pretty

object TagPatternVersionSpec extends Properties("TagPatternVersionSpec") {
  val repoStates = new RepoStates(tagPrefix = "")
  import repoStates._

  property("not a git repo")                               = notAGitRepo().version()          ?= "HEAD+20160917-0000"
  property("no commits")                                   = noCommits().version()            ?= "HEAD+20160917-0000"
  property("on commit, w/o local changes")                 = onCommit().version()            ??= "0.0.0+3-1234abcd"
  property("on commit with local changes")                 = onCommitDirty().version()       ??= "0.0.0+3-1234abcd+20160917-0000"
  property("on tag 1.0.0, w/o local changes")              = onTag().version()                ?= "1.0.0"
  property("on tag 1.0.0 with local changes")              = onTagDirty().version()           ?= "1.0.0+0-1234abcd+20160917-0000"
  property("on tag 1.0.0 and 1 commit, w/o local changes") = onTagAndCommit().version()       ?= "1.0.0+1-1234abcd"
  property("on tag 1.0.0 and 1 commit with local changes") = onTagAndCommitDirty().version()  ?= "1.0.0+1-1234abcd+20160917-0000"

  implicit class LazyAnyOps[A](x: => A)(implicit ev: A => Pretty) {
    def ??=(y: A) = {
      if (x == y) passed // the standard "?=" uses "proved" while we want to run multiple times
      else falsified :| s"Expected ${pretty(y)} but got ${pretty(x)}"
    }
  }

  override def overrideParameters(p: Test.Parameters) =
    p.withMinSuccessfulTests(3) // .. but not 100 times!
}
