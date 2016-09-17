package sbtdynver

import java.util._

import scala.util._

import sbt._, Keys._

object DynVerPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  object autoImport {
    val dynverCurrentDate = settingKey[Date]("The current date, for dynver purposes")
  }
  import autoImport._

  private val dynver = DynVer(None)

  override def buildSettings = Seq(
       version := dynver.version(dynverCurrentDate.value),
    isSnapshot := dynver.isSnapshot(),

    dynverCurrentDate := new Date
  )
}

final case class DynVer(wd: Option[File]) {
  def version(d: Date): String = makeDynVer(d) getOrElse s"HEAD+${currentYearMonthDay(d)}"
  def isSnapshot(): Boolean    = isDirty() || hasNoTags()

  def currentYearMonthDay(d: Date): String = "%1$tY%1$tm%1$td" format d

  def makeDynVer(d: Date): Option[String] = {
    Try(Process(s"""git describe --abbrev=8 --match v[0-9].* --always --dirty=+${currentYearMonthDay(d)}""", wd).!!)
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
