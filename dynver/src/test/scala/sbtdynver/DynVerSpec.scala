package sbtdynver

import org.scalacheck._, Prop._

import RepoStates._

object VersionSpec extends Properties("VersionSpec") {
  property("not a git repo")                                = notAGitRepo          .version ?= "HEAD+20160917-0000"
  property("no commits")                                    = noCommits            .version ?= "HEAD+20160917-0000"
  property("on commit, w/o local changes")                  = onCommit             .version ?= "0.0.0+3-1234abcd"
  property("on commit with local changes")                  = onCommit.dirty       .version ?= "0.0.0+3-1234abcd+20160917-0000"
  property("on tag v1.0.0, w/o local changes")              = onTag                .version ?= "1.0.0"
  property("on tag v1.0.0 with local changes")              = onTag.dirty          .version ?= "1.0.0+0-1234abcd+20160917-0000"
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTag.commit         .version ?= "1.0.0+1-1234abcd"
  property("on tag v1.0.0 and 1 commit with local changes") = onTag.commit.dirty   .version ?= "1.0.0+1-1234abcd+20160917-0000"
  property("on tag v2")                                     = onTag.commit.tag("2").version ?= "2" // #7, didn't match
}

object PreviousVersionSpec extends Properties("PreviousVersionSpec") {
  property("not a git repo")                                = notAGitRepo              .previousVersion ?= None
  property("no commits")                                    = noCommits                .previousVersion ?= None
  property("on commit, w/o local changes")                  = onCommit                 .previousVersion ?= None
  property("on commit with local changes")                  = onCommit.dirty           .previousVersion ?= None
  property("on tag v1.0.0, w/o local changes")              = onTag                    .previousVersion ?= None
  property("on tag v1.0.0 with local changes")              = onTag.dirty              .previousVersion ?= None
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTag.commit             .previousVersion ?= Some("1.0.0")
  property("on tag v1.0.0 and 1 commit with local changes") = onTag.commit.dirty       .previousVersion ?= Some("1.0.0")
  property("on tag v2.0.0, w/o local changes")              = onTag.commit.tag("2.0.0").previousVersion ?= Some("1.0.0")

  property("w/ merge commits")            = onBranch2x.checkout("master").merge("2.x") .previousVersion ?= Some("1.0.0")
  property("w/ merge commits + tag")      = onBranch2Tag                               .previousVersion ?= Some("1.0.0")
  property("on multiple branches")        = onMultiBranch.checkout("2.x")              .previousVersion ?= Some("2.0.0")
  property("on merging branch with tags") = onMultiBranch.merge("2.x")                 .previousVersion ?= Some("1.1.0")
  property("with non-version prev tag")   = onTag.commit.tag("hm").commit.tag("v2.0.0").previousVersion ?= Some("1.0.0")
}

object IsSnapshotSpec extends Properties("IsSnapshotSpec") {
  property("not a git repo")                                = notAGitRepo          .isSnapshot ?= true
  property("no commits")                                    = noCommits            .isSnapshot ?= true
  property("on commit, w/o local changes")                  = onCommit             .isSnapshot ?= true
  property("on commit with local changes")                  = onCommit.dirty       .isSnapshot ?= true
  property("on tag v1.0.0, w/o local changes")              = onTag                .isSnapshot ?= false
  property("on tag v1.0.0 with local changes")              = onTag.dirty          .isSnapshot ?= true
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTag.commit         .isSnapshot ?= true
  property("on tag v1.0.0 and 1 commit with local changes") = onTag.commit.dirty   .isSnapshot ?= true
  property("on tag v2")                                     = onTag.commit.tag("2").isSnapshot ?= false
}

object SonatypeSnapshotSpec extends Properties("SonatypeSnapshotSpec") {
  property("on tag v1.0.0 with local changes")                = onTag.dirty          .sonatypeVersion ?= "1.0.0+0-1234abcd+20160917-0000-SNAPSHOT"
  property("on tag v1.0.0 and 1 commit, w/o local changes S") = onTag.commit         .sonatypeVersion ?= "1.0.0+1-1234abcd-SNAPSHOT"
  property("on tag v1.0.0, w/o local changes")                = onTag                .sonatypeVersion ?= "1.0.0"
  property("on tag v2")                                       = onTag.commit.tag("2").sonatypeVersion ?= "2"
}

object isVersionStableSpec extends Properties("IsVersionStableSpec") {
  property("not a git repo")                                = notAGitRepo          .isVersionStable ?= false
  property("no commits")                                    = noCommits            .isVersionStable ?= false
  property("on commit, w/o local changes")                  = onCommit             .isVersionStable ?= true
  property("on commit with local changes")                  = onCommit.dirty       .isVersionStable ?= false
  property("on tag v1.0.0, w/o local changes")              = onTag                .isVersionStable ?= true
  property("on tag v1.0.0 with local changes")              = onTag.dirty          .isVersionStable ?= false
  property("on tag v1.0.0 and 1 commit, w/o local changes") = onTag.commit         .isVersionStable ?= true
  property("on tag v1.0.0 and 1 commit with local changes") = onTag.commit.dirty   .isVersionStable ?= false
  property("on tag v2")                                     = onTag.commit.tag("2").isVersionStable ?= true
}
