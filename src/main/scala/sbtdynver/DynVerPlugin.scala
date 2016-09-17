package sbtdynver

import java.util._

import scala.util._

import sbt._, Keys._

object DynVerPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  private val dynver = DynVer(None, RealClock)

  override def buildSettings = Seq(
       version := dynver.version(),
    isSnapshot := dynver.isSnapshot()
  )
}

final case class DynVer(wd: Option[File], clock: Clock) {
  def version(): String     = makeDynVer() getOrElse s"HEAD+$currentYearMonthDay"
  def isSnapshot(): Boolean = isDirty() || hasNoTags()

  def currentYearMonthDay(): String = "%1$tY%1$tm%1$td" format clock.now()

  def makeDynVer(): Option[String] = {
    Try(Process(s"""git describe --abbrev=8 --match v[0-9].* --always --dirty=+$currentYearMonthDay""", wd).!!)
      .toOption.map(_
        .init
        .replaceAll("^v", "")
        .replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2")
      )
  }

  def isDirty(): Boolean =
    Try(Process("git status --untracked-files=no --porcelain", wd).!!).map(_.nonEmpty).getOrElse(true)

  def hasNoTags(): Boolean =
    Try(Process("git for-each-ref --format %(objecttype) refs/tags/", wd).!!)
      .map(_.linesIterator.forall(_ startsWith "commit"))
      .getOrElse(true)
}

abstract class Clock private[sbtdynver]() {
  def now(): Date
}

object RealClock extends Clock {
  def now(): Date = new Date()
}
