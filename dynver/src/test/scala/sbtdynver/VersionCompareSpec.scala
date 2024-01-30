package sbtdynver

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.propBoolean
import org.scalacheck.Properties

object VersionCompareSpec extends Properties("VersionCompareSpec") {
  property("major version is higher than minors") = forAll(Gen.posNum[Int]){ (i:Int) =>
    DynVer.versionCompare(GitRef(s"v${i-1}.$i.$i"), GitRef(s"$i.${i+1}.${i+1}")) &&
    DynVer.versionCompare(GitRef(s"${i-1}.$i.$i"), GitRef(s"v$i.${i+1}.${i+1}")) &&
      DynVer.versionCompare(GitRef(s"v$i.$i.$i"), GitRef(s"$i.${i+1}.${i+1}")) &&
      DynVer.versionCompare(GitRef(s"$i.$i.$i"), GitRef(s"v$i.${i+1}.${i+1}"))
  }

  property("not really semver versions are supported") = DynVer.versionCompare(GitRef(s"9"), GitRef(s"v10"))

  property("not really semver versions that are not even in length are supported") =
    forAll{ (i: Int) => DynVer.versionCompare(GitRef(s"9"), GitRef(s"v10.$i")) }

  property("not really versions with qualifier are supported") = DynVer.versionCompare(GitRef(s"9-SNAPSHOT"), GitRef(s"v10"))

  property("qualifiers are supported") = forAll(Gen.alphaNumStr){ (qualifier:String) =>
    qualifier.nonEmpty ==> DynVer.versionCompare(GitRef(s"9.2.2-$qualifier"), GitRef(s"v10"))
  }
}