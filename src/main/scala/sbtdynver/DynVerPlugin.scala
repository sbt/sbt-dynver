package sbtdynver

import java.util._

import scala.util._

import sbt._, Keys._

object DynVerPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  object autoImport {
    val dynver              = taskKey[String]("The version of your project, from git")
    val dynverCurrentDate   = settingKey[Date]("The current date, for dynver purposes")
    val dynverCheckVersion  = taskKey[Boolean]("Check if version and dynver match")
    val dynverAssertVersion = taskKey[Unit]("Assert if version and dynver match")
  }
  import autoImport._

  override def buildSettings = Seq(
       version := DynVer(None).version(dynverCurrentDate.value),
    isSnapshot := DynVer(None).isSnapshot(),

    dynver              := DynVer(None).version(new Date),
    dynverCurrentDate   := new Date,
    dynverCheckVersion  := (dynver.value == version.value),
    dynverAssertVersion := (if (!dynverCheckVersion.value)
      sys.error(s"Version and dynver mismatch - version: ${version.value}, dynver: ${dynver.value}")
    )
  )
}

final case class DynVer(wd: Option[File]) {
  def version(d: Date): String = makeDynVer(d) getOrElse s"HEAD+${timestamp(d)}"
  def isSnapshot(): Boolean    = isDirty() || hasNoTags()

  def timestamp(d: Date): String = "%1$tY%1$tm%1$td-%1$tH%1$tM" format d

  def makeDynVer(d: Date): Option[String] = {
    val tstamp = timestamp(d)
    Try(
      Process(s"""git describe --abbrev=8 --match v[0-9].* --always --dirty=+$tstamp""", wd).!!(NoProcessLogger)
    ).toOption.map(_
      .init
      .replaceAll("^v", "")
      .replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2")
    )
  }

  def isDirty(): Boolean =
    Try(Process("git status --untracked-files=no --porcelain", wd).!!(NoProcessLogger))
      .map(_.nonEmpty)
      .getOrElse(true)

  def hasNoTags(): Boolean =
    Try(Process("git for-each-ref --format %(objecttype) refs/tags/", wd).!!(NoProcessLogger))
      .map(_.linesIterator.forall(_ startsWith "commit"))
      .getOrElse(true)
}

object NoProcessLogger extends ProcessLogger {
  def info(s: =>String)  = ()
  def error(s: =>String) = ()
  def buffer[T](f: => T) = f
}
