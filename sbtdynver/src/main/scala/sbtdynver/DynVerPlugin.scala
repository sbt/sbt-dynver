package sbtdynver

import java.util._

import sbt._
import sbt.Keys._

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
    val dynverTagPrefix                = settingKey[String]("The prefix to use when matching the version tag")
    val dynverVTagPrefix               = settingKey[Boolean]("Whether or not tags have a 'v' prefix")
    val dynverCheckVersion             = taskKey[Boolean]("Checks if version and dynver match")
    val dynverAssertVersion            = taskKey[Unit]("Asserts if version and dynver match")

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

  override def buildSettings = Seq(
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

    dynverInstance := {
      val vTagPrefix = dynverVTagPrefix.value
      val tagPrefix = dynverTagPrefix.?.value.getOrElse(if (vTagPrefix) "v" else "")
      assert(vTagPrefix ^ tagPrefix != "v", s"Incoherence: dynverTagPrefix=$tagPrefix vs dynverVTagPrefix=$vTagPrefix")
      DynVer(Some(buildBase.value), dynverSeparator.value, tagPrefix)
    },

    dynverCurrentDate              := new Date,
    dynverGitDescribeOutput        := dynverInstance.value.getGitDescribeOutput(dynverCurrentDate.value),
    dynverSonatypeSnapshots        := false,
    dynverGitPreviousStableVersion := dynverInstance.value.getGitPreviousStableTag,
    dynverSeparator                := DynVer.separator,
    dynverVTagPrefix               := dynverTagPrefix.??(DynVer.tagPrefix).value == "v",

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
