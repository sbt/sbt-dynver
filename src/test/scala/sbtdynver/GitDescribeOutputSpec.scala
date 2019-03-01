package sbtdynver

import org.scalacheck._, Prop._

object GitDescribeOutputSpec extends Properties("GitDescribeOutputSpec") {

  test("v1.0.0",                          "v1.0.0",   0, "",         ""              )
  test("v1.0.0+20140707-1030",            "v1.0.0",   0, "",         "20140707-1030")
  test("v1.0.0+3-1234abcd",               "v1.0.0",   3, "1234abcd", ""              )
  test("v1.0.0+3-1234abcd+20140707-1030", "v1.0.0",   3, "1234abcd", "20140707-1030")
  test("1234abcd",                        "1234abcd", 0, "",         ""              )
  test("1234abcd+20140707-1030",          "1234abcd", 0, "",         "20140707-1030")
  test("HEAD+20140707-1030",              "HEAD",     0, "",         "20140707-1030")

  def test(v: String, ref: String, dist: Int, sha: String, dirtySuffix: String) = {
    val out = GitDescribeOutput(GitRef(ref), GitCommitSuffix(dist, sha), GitDirtySuffix(dirtySuffix))
    property(s"parses $v") = GitDescribeOutput.parse(v) ?= out
  }
}
