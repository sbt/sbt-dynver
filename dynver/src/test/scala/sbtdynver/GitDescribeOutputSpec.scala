package sbtdynver

import org.scalacheck._, Prop._

object GitDescribeOutputSpec extends Properties("GitDescribeOutputSpec") {
  test("v1.0.0",                              "v1.0.0",   0, "",         ""              )
  test("v1.0.0+20140707-1030",                "v1.0.0",   0, "",         "+20140707-1030")
  test("v1.0.0+3-1234abcd",                   "v1.0.0",   3, "1234abcd", ""              )
  test("v1.0.0+3-1234abcd+20140707-1030",     "v1.0.0",   3, "1234abcd", "+20140707-1030")
  test("v1.0.0+3-1234abcd+20140707-1030\r\n", "v1.0.0",   3, "1234abcd", "+20140707-1030", "handles CR+LF (Windows)") // #20, didn't match
  test("1234abcd",                            "1234abcd", 0, "",         ""              )
  test("1234abcd+20140707-1030",              "1234abcd", 0, "",         "+20140707-1030")
  test("HEAD+20140707-1030",                  "HEAD",     0, "",         "+20140707-1030")

  def test(v: String, ref: String, dist: Int, sha: String, dirtySuffix: String, propName: String = null) = {
    val out = GitDescribeOutput(GitRef(ref), GitCommitSuffix(dist, sha), GitDirtySuffix(dirtySuffix))
    val propName1 = if (propName == null) s"parses $v" else propName
    property(propName1) = DynVer.parser.parse(v) ?= out
  }
}
