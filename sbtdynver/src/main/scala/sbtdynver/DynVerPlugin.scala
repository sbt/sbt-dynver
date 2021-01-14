package sbtdynver

import java.util.Date

import sbt._, Keys._, plugins.JvmPlugin

object DynVerPlugin extends AutoPlugin {
  override def requires = JvmPlugin
  override def trigger  = allRequirements

  object autoImport {
    val dynver = taskKey[String]("The version of your project, from git")

    val dynverVTagPrefix        = settingKey[Boolean]("Whether or not tags have a 'v' prefix")
    val dynverTagPrefix         = settingKey[String]("The prefix to use when matching the version tag")
    val dynverSeparator         = settingKey[String]("The separator to use between tag and distance, and the hash and dirty timestamp")
    val dynverCurrentDate       = settingKey[Date]("The current date, for dynver purposes")
    val dynverSonatypeSnapshots = settingKey[Boolean]("Whether to append -SNAPSHOT to snapshot versions")

    val dynverInstance                 = settingKey[DynVer]("The dynver instance for this build")
    val dynverGitDescribeOutput        = settingKey[Option[GitDescribeOutput]]("The output from git describe")
    val dynverGitPreviousStableVersion = settingKey[Option[GitDescribeOutput]]("The last stable tag")

    val isVersionStable        = settingKey[Boolean]("The version string identifies a specific point in version control, so artifacts built from this version can be safely cached")
    val previousStableVersion  = settingKey[Option[String]]("The last stable version as seen from the current commit (does not include the current commit's version/tag)")

    val dynverAssertTagVersion = assertTagVersion // Asserts if the version derives from git tags
    val dynverCheckVersion     = taskKey[Boolean]("Checks if version and dynver match")
    val dynverAssertVersion    = taskKey[Unit]("Asserts if version and dynver match")
  }
  import autoImport._

  override def buildSettings = Seq(
    version := getVersion.value(dynverCurrentDate.value, dynverGitDescribeOutput.value),
    dynver  := getVersion.value(new Date, dynverInstance.value.getGitDescribeOutput(new Date)),

    dynverVTagPrefix        := dynverTagPrefix.??(DynVer.tagPrefix).value == "v",
    dynverSeparator         := DynVer.separator,
    dynverCurrentDate       := new Date,
    dynverSonatypeSnapshots := false,

    dynverInstance                 := DynVer(Some(buildBase.value), dynverSeparator.value, tagPrefix.value),
    dynverGitDescribeOutput        := dynverInstance.value.getGitDescribeOutput(dynverCurrentDate.value),
    dynverGitPreviousStableVersion := dynverInstance.value.getGitPreviousStableTag,

    isSnapshot             := dynverGitDescribeOutput.value.isSnapshot,
    isVersionStable        := dynverGitDescribeOutput.value.isVersionStable,
    previousStableVersion  := dynverGitPreviousStableVersion.value.previousVersion,

    dynverCheckVersion     := (dynver.value == version.value),
    dynverAssertVersion    := assertVersionImpl.value,
  )

  private val getVersion = Def.setting { (date: Date, out: Option[GitDescribeOutput]) =>
    out.getVersion(date, dynverSeparator.value, dynverSonatypeSnapshots.value)
  }

  private val tagPrefix = Def.setting {
    val vTagPrefix = dynverVTagPrefix.value
    val  tagPrefix = dynverTagPrefix.?.value.getOrElse(if (vTagPrefix) "v" else "")
    assert(vTagPrefix ^ tagPrefix != "v", s"Incoherence: dynverTagPrefix=$tagPrefix vs dynverVTagPrefix=$vTagPrefix")
    tagPrefix
  }

  private val assertTagVersion = Def.setting {
    dynverGitDescribeOutput.value.assertTagVersion(version.value)
  }

  private val assertVersionImpl = Def.task {
    val v = version.value
    val dv = dynver.value
    if (!dynverCheckVersion.value)
      sys.error(s"Version and dynver mismatch - version: $v, dynver: $dv")
  }

  private val buildBase = baseDirectory in ThisBuild
}
