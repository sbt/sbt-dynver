package sbtdynver

import sbt._, Keys._

object DynverPlugin extends AutoPlugin {
  // TODO: Test if this requirement can be lowered
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  override def buildSettings = Seq(
    version := {
      def overrideVersion = Option(sys props "project.version")
      def   dynverVersion = Some(makeDynVer())
      def    datedVersion = s"HEAD+$currentYearMonthDay"

      Seq(overrideVersion, dynverVersion) reduce (_ orElse _) getOrElse datedVersion
    },
    isSnapshot := isDirty() || hasNoTags()
  )

  def currentYearMonthDay() = "%1$tY%1$tm%1$td" format new java.util.Date

  def makeDynVer() = {
    s"""git describe --abbrev=8 --match v[0-9].* --always --dirty=+$currentYearMonthDay""".!!.init
      .replaceAll("^v", "")
      .replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2")
  }

  def isDirty() = "git status --untracked-files=no --porcelain".!!.nonEmpty

  def hasNoTags() = "git for-each-ref --format %(objecttype) refs/tags/".!!
    .linesIterator.forall(_ startsWith "commit")
}
