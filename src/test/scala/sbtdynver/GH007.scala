package sbtdynver

import org.scalacheck._, Prop._

import RepoStates._

object GH007 extends Properties("GH6") {
  property("A tag of v2 is matched") = onTag("v2").version() ?= "2"
}
