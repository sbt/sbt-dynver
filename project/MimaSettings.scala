import sbt._, Keys._
import sbt.Classpaths.pluginProjectID

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters.exclude
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object MimaSettings {
  // clear out mimaBinaryIssueFilters when changing this
  val mimaPreviousVersion = "5.0.0-M3"

  val projID = Def.setting {
    // Using projectID something is wrong... Looks for dynver_2.12 but artifacts are name=dynver
    // Even pluginProjectID.value.withExplicitArtifacts(Vector())) doesn't work here..?
    val sbtBv = (pluginCrossBuild /   sbtBinaryVersion).value
    val sbv   = (pluginCrossBuild / scalaBinaryVersion).value
    val mid   = organization.value %% moduleName.value % version.value
    if (sbtPlugin.value) Defaults.sbtPluginExtra(mid, sbtBv, sbv) else mid
  }

  val mimaSettings = Def.settings (
    mimaPreviousArtifacts        := Set.empty, // Set(projID.value.withRevision(mimaPreviousVersion)),
    mimaReportSignatureProblems  := true,
    mimaBinaryIssueFilters      ++= Seq(
      ProblemFilters.exclude[Problem]("*.impl.*"), // KEEP: impl is for internal implementation details
    ),
  )
}
