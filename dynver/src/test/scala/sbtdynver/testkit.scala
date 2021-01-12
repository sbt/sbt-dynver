package sbtdynver

import org.scalacheck._, Prop._, util.Pretty, Pretty.pretty

object testkit {
  implicit class LazyAnyOps[A](x: => A)(implicit ev: A => Pretty) {
    def ??=(y: A) = {
      if (x == y) passed // the standard "?=" uses "proved" while we want to run multiple times
      else falsified :| s"Expected ${pretty(y)} but got ${pretty(x)}"
    }
  }
}
