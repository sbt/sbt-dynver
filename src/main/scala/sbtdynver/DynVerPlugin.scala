package sbtdynver

import sbt._, Keys._

object DynverPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  private val dynver = DynVer(None)

  override def buildSettings = Seq(
       version := dynver.version(),
    isSnapshot := dynver.isSnapshot()
  )
}

final case class DynVer(wd: Option[File]) {
  def version() = {
    def overrideVersion = Option(sys props "project.version")
    def   dynverVersion = Some(makeDynVer())
    def    datedVersion = s"HEAD+$currentYearMonthDay"

    Seq(overrideVersion, dynverVersion) reduce (_ orElse _) getOrElse datedVersion
  }

  def isSnapshot() = isDirty() || hasNoTags()

  def currentYearMonthDay() = "%1$tY%1$tm%1$td" format new java.util.Date

  def makeDynVer() = {
    Process(s"""git describe --abbrev=8 --match v[0-9].* --always --dirty=+$currentYearMonthDay""", wd).!!.init
      .replaceAll("^v", "")
      .replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2")
  }

  def isDirty() = Process("git status --untracked-files=no --porcelain", wd).!!.nonEmpty

  def hasNoTags() = Process("git for-each-ref --format %(objecttype) refs/tags/", wd).!!
    .linesIterator.forall(_ startsWith "commit")
}
