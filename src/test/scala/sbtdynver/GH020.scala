package sbtdynver

import org.scalacheck._, Prop._

object GH020 extends Properties("GH020") {
  property("Handles CF+LF (Windows)") =
    GitDescribeOutput.parse("v0.7.0+3-e7a84ebc+20161120-1948\r\n") ?=
      GitDescribeOutput(GitRef("v0.7.0"), GitCommitSuffix(3, "e7a84ebc"), GitDirtySuffix("20161120-1948"))
}
