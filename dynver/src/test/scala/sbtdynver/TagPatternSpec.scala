package sbtdynver

import org.scalacheck._, Prop._
import testkit._

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

  override def overrideParameters(p: Test.Parameters) =
    p.withMinSuccessfulTests(3) // no need to run 100 times!
}
