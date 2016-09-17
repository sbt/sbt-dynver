import org.scalacheck._, Prop._

object DynVerSpec extends Properties("DynVerSpec") {
  property("when on v1.0.0 tag, w/o local changes") = passed
  property("when on v1.0.0 tag with local changes") = passed
  property("when on commit 1234abcd: 3 commits after v1.0.0 tag, w/o local changes") = passed
  property("when on commit 1234abcd: 3 commits after v1.0.0 tag with local changes") = passed
  property("when there are no tags, on commit 1234abcd, w/o local changes") = passed
  property("when there are no tags, on commit 1234abcd with local changes") = passed
  property("when there are no commits") = passed
  property("when not a git repo") = passed
}
