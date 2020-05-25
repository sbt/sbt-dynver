package sbtdynver

import java.util._

import scala.{ PartialFunction => ?=> }
import scala.util._

import sbt._
import sbt.Keys._

import scala.sys.process.{ Process, ProcessLogger }

object DynVerPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  object autoImport {
    val dynver                         = taskKey[String]("The version of your project, from git")
    val dynverInstance                 = settingKey[DynVer]("The dynver instance for this build")
    val dynverCurrentDate              = settingKey[Date]("The current date, for dynver purposes")
    val dynverGitDescribeOutput        = settingKey[Option[GitDescribeOutput]]("The output from git describe")
    val dynverSonatypeSnapshots        = settingKey[Boolean]("Whether to append -SNAPSHOT to snapshot versions")
    val dynverGitPreviousStableVersion = settingKey[Option[GitDescribeOutput]]("The last stable tag")
    val dynverSeparator                = settingKey[String]("The separator to use between tag and distance, and the hash and dirty timestamp")
    val dynverVTagPrefix               = settingKey[Boolean]("Whether or not tags have a 'v' prefix")
    val dynverCheckVersion             = taskKey[Boolean]("Checks if version and dynver match")
    val dynverAssertVersion            = taskKey[Unit]("Asserts if version and dynver match")
    val dynverTagPrefix                = settingKey[String]("The prefix to use when matching the verision tag")

    // Asserts if the version derives from git tags
    val dynverAssertTagVersion         = Def.setting {
      val v = version.value
      if (dynverGitDescribeOutput.value.hasNoTags)
        throw new MessageOnlyException(
          s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
        )
    }

    // Would be nice if this were an 'upstream' key
    val isVersionStable         = settingKey[Boolean]("The version string identifies a specific point in version control, so artifacts built from this version can be safely cached")
    val previousStableVersion   = settingKey[Option[String]]("The last stable version as seen from the current commit (does not include the current commit's version/tag)")
  }
  import autoImport._

  override def projectSettings = Seq(
    version := {
      val out = dynverGitDescribeOutput.value
      val date = dynverCurrentDate.value
      val separator = dynverSeparator.value
      if (dynverSonatypeSnapshots.value) out.sonatypeVersionWithSep(date, separator)
      else out.versionWithSep(date, separator)
    },
    isSnapshot              := dynverGitDescribeOutput.value.isSnapshot,
    isVersionStable         := dynverGitDescribeOutput.value.isVersionStable,
    previousStableVersion   := dynverGitPreviousStableVersion.value.previousVersion,

    dynverTagPrefix                := DynVer.tagPrefix,
    dynverCurrentDate              := new Date,
    dynverInstance                 := DynVer(Some(buildBase.value), dynverSeparator.value, dynverTagPrefix.value),
    dynverGitDescribeOutput        := dynverInstance.value.getGitDescribeOutput(dynverCurrentDate.value),
    dynverSonatypeSnapshots        := false,
    dynverGitPreviousStableVersion := dynverInstance.value.getGitPreviousStableTag,
    dynverSeparator                := DynVer.separator,
    dynverVTagPrefix               := DynVer.tagPrefix == "v",

    dynver                  := {
      val dynver = dynverInstance.value
      if (dynverSonatypeSnapshots.value) dynver.sonatypeVersion(new Date)
      else dynver.version(new Date)
    },
    dynverCheckVersion      := (dynver.value == version.value),
    dynverAssertVersion     := {
      val v = version.value
      val dv = dynver.value
      if (!dynverCheckVersion.value)
        sys.error(s"Version and dynver mismatch - version: $v, dynver: $dv")
    }
  )

  private val buildBase = baseDirectory in ThisBuild
}

final case class GitRef(value: String)
final case class GitCommitSuffix(distance: Int, sha: String)
final case class GitDirtySuffix(value: String)

object GitRef extends (String => GitRef) {
  final implicit class GitRefOps(val x: GitRef) extends AnyVal { import x._
    def isTag: Boolean = value startsWith "v"
    def dropV: GitRef = GitRef(value.replaceAll("^v", ""))
    def mkString(prefix: String, suffix: String): String = if (value.isEmpty) "" else prefix + value + suffix
  }
}

object GitCommitSuffix extends ((Int, String) => GitCommitSuffix) {
  final implicit class GitCommitSuffixOps(val x: GitCommitSuffix) extends AnyVal { import x._
    def isEmpty: Boolean = distance <= 0 || sha.isEmpty
    def mkString(prefix: String, infix: String, suffix: String): String =
      if (sha.isEmpty) "" else prefix + distance + infix + sha + suffix
  }
}

object GitDirtySuffix extends (String => GitDirtySuffix) {
  final implicit class GitDirtySuffixOps(val x: GitDirtySuffix) extends AnyVal { import x._
    def dropPlus: GitDirtySuffix = GitDirtySuffix(value.replaceAll("^\\+", ""))
    def withSeparator(separator: String): String = dropPlus.mkString(separator, "")
    def mkString(prefix: String, suffix: String): String = if (value.isEmpty) "" else prefix + value + suffix
  }
}

final case class GitDescribeOutput(ref: GitRef, commitSuffix: GitCommitSuffix, dirtySuffix: GitDirtySuffix) {
  def version(sep: String): String = {
    val dirtySuffix = this.dirtySuffix.withSeparator(sep)
    if (isCleanAfterTag) ref.dropV.value + dirtySuffix // no commit info if clean after tag
    else if (commitSuffix.sha.nonEmpty) ref.dropV.value + sep + commitSuffix.distance + "-" + commitSuffix.sha + dirtySuffix
    else "0.0.0" + sep + commitSuffix.distance + "-" + ref.value + dirtySuffix
  }

  def sonatypeVersion(sep: String): String = if (isSnapshot) version(sep) + "-SNAPSHOT" else version(sep)

  def version: String            = version(DynVer.separator)
  def sonatypeVersion: String    = sonatypeVersion(DynVer.separator)

  def isSnapshot(): Boolean      = hasNoTags() || !commitSuffix.isEmpty || isDirty()
  def previousVersion: String    = ref.dropV.value
  def isVersionStable(): Boolean = !isDirty()

  def hasNoTags(): Boolean       = !ref.isTag
  def isDirty(): Boolean         = dirtySuffix.value.nonEmpty
  def isCleanAfterTag: Boolean   = ref.isTag && commitSuffix.isEmpty && !isDirty()
}

object GitDescribeOutput extends ((GitRef, GitCommitSuffix, GitDirtySuffix) => GitDescribeOutput) {
  private val OptWs        =  """[\s\n]*""" // doesn't \s include \n? why can't this call .r?
  private val Distance     =  """\+([0-9]+)""".r
  private val Sha          =  """([0-9a-f]{8})""".r
  private val HEAD         =  """HEAD""".r
  private val CommitSuffix = s"""($Distance-$Sha)""".r
  private val TstampSuffix =  """(\+[0-9]{8}-[0-9]{4})""".r

  private[sbtdynver] final class Parser(tagPrefix: String) {
    private val Tag = s"""($tagPrefix[0-9][^+]*?)""".r

    private val FromTag  = s"""^$OptWs$Tag$CommitSuffix?$TstampSuffix?$OptWs$$""".r
    private val FromSha  = s"""^$OptWs$Sha$TstampSuffix?$OptWs$$""".r
    private val FromHead = s"""^$OptWs$HEAD$TstampSuffix$OptWs$$""".r

    private[sbtdynver] def parse: String ?=> GitDescribeOutput = {
      case FromTag(tag, _, dist, sha, dirty) => parse0(v(tag), dist, sha, dirty)
      case FromSha(sha, dirty)               => parse0(   sha,  "0",  "", dirty)
      case FromHead(dirty)                   => parse0("HEAD",  "0",  "", dirty)
    }

    // If tags aren't v-prefixed, add the v back, so the rest of dynver works (e.g. GitRef#isTag)
    private def v(s: String) = s"$tagPrefix$s"

    private def parse0(ref: String, dist: String, sha: String, dirty: String) = {
      val commit = if (dist == null || sha == null) GitCommitSuffix(0, "") else GitCommitSuffix(dist.toInt, sha)
      GitDescribeOutput(GitRef(ref), commit, GitDirtySuffix(if (dirty eq null) "" else dirty))
    }
  }

  implicit class OptGitDescribeOutputOps(val _x: Option[GitDescribeOutput]) extends AnyVal {
    def mkVersion(f: GitDescribeOutput => String, fallback: => String): String = _x.fold(fallback)(f)

    def version(d: Date): String          = versionWithSep(d, DynVer.separator)
    def sonatypeVersion(d: Date): String  = sonatypeVersionWithSep(d, DynVer.separator)

    // overloading isn't bincompat :O
    def versionWithSep(d: Date, sep: String): String         = mkVersion(_.version(sep), fallback(sep, d))
    def sonatypeVersionWithSep(d: Date, sep: String): String = mkVersion(_.sonatypeVersion(sep), fallback(sep, d))

    def previousVersion: Option[String]   = _x.map(_.previousVersion)
    def isSnapshot: Boolean               = _x.forall(_.isSnapshot)
    def isVersionStable: Boolean          = _x.exists(_.isVersionStable)

    def isDirty: Boolean         = _x.fold(true)(_.isDirty())
    def hasNoTags: Boolean       = _x.fold(true)(_.hasNoTags())
  }

  private[sbtdynver] def timestamp(d: Date): String = "%1$tY%1$tm%1$td-%1$tH%1$tM" format d
  private[sbtdynver] def fallback(separator: String, d: Date) = s"HEAD$separator${timestamp(d)}"
}

// sealed just so the companion object can extend it. Shouldn't've been a case class.
sealed case class DynVer(wd: Option[File], separator: String, tagPrefix: String) {
  private def this(wd: Option[File], separator: String) = this(wd, separator, "v")
  private def this(wd: Option[File]) = this(wd, "+")

  private val TagPattern = s"$tagPrefix[0-9]*"
  private[sbtdynver] val parser = new GitDescribeOutput.Parser(tagPrefix)

  def version(d: Date): String            = getGitDescribeOutput(d).versionWithSep(d, separator)
  def sonatypeVersion(d: Date): String    = getGitDescribeOutput(d).sonatypeVersionWithSep(d, separator)
  def previousVersion : Option[String]    = getGitPreviousStableTag.previousVersion
  def isSnapshot(): Boolean               = getGitDescribeOutput(new Date).isSnapshot
  def isVersionStable(): Boolean          = getGitDescribeOutput(new Date).isVersionStable

  def makeDynVer(d: Date): Option[String] = getGitDescribeOutput(d) map (_.version(separator))
  def isDirty(): Boolean                  = getGitDescribeOutput(new Date).isDirty
  def hasNoTags(): Boolean                = getGitDescribeOutput(new Date).hasNoTags

  def getDistanceToFirstCommit(): Option[Int] = {
    val process = Process(s"git rev-list --count HEAD", wd)
    Try(process !! impl.NoProcessLogger).toOption
      .map(_.trim.toInt)
  }

  def getGitDescribeOutput(d: Date): Option[GitDescribeOutput] = {
    val process = Process(s"git describe --long --tags --abbrev=8 --match $TagPattern --always --dirty=+${timestamp(d)}", wd)
    Try(process !! impl.NoProcessLogger).toOption
      .map(_.stripPrefix(tagPrefix))
      .map(_.replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2"))
      .map(parser.parse)
      .flatMap(output =>
        if (output.hasNoTags) getDistanceToFirstCommit().map(dist =>
          output.copy(commitSuffix = output.commitSuffix.copy(distance = dist))
        )
        else Some(output)
      )
  }

  def getGitPreviousStableTag: Option[GitDescribeOutput] = {
    for {
      // Find the parent of the current commit. The "^1" instructs it to show only the first parent,
      // as merge commits can have multiple parents
      parentHash <- execAndHandleEmptyOutput("git --no-pager log --pretty=%H -n 1 HEAD^1")
      // Find the closest tag of the parent commit
      tag <- execAndHandleEmptyOutput(s"git describe --tags --abbrev=0 --match $TagPattern --always $parentHash")
      out <- PartialFunction.condOpt(tag)(parser.parse)
    } yield out
  }

  def timestamp(d: Date): String = GitDescribeOutput.timestamp(d)
  def fallback(d: Date): String = GitDescribeOutput.fallback(separator, d)

  private def execAndHandleEmptyOutput(cmd: String): Option[String] = {
    Try(Process(cmd, wd) !! impl.NoProcessLogger).toOption
      .filter(_.trim.nonEmpty)
  }

  def copy(wd: Option[File] = wd): DynVer = new DynVer(wd, separator)
}

object DynVer extends DynVer(None) with (Option[File] => DynVer) {
  override def apply(wd: Option[File]) = apply(wd, separator, tagPrefix)
}

object `package`

package impl {
  object NoProcessLogger extends ProcessLogger {
    def info(s: => String)  = ()
    def out(s: => String)   = ()
    def error(s: => String) = ()
    def err(s: => String)   = ()
    def buffer[T](f: => T)  = f
  }
}
