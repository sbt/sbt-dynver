val dynverRoot = project.in(file("."))
val dynverP = LocalProject("dynver")
aggregateProjects(dynverP, sbtdynver)

inThisBuild(List(
  organization := "com.dwijnand",
      licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
   description := "An sbt plugin to dynamically set your version from git",
    developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", url("https://dwijnand.com"))),
     startYear := Some(2016),
      homepage := scmInfo.value map (_.browseUrl),
       scmInfo := Some(ScmInfo(url("https://github.com/dwijnand/sbt-dynver"), "scm:git:git@github.com:dwijnand/sbt-dynver.git")),

            Global /      sbtVersion  := "1.0.0", // must be Global, otherwise ^^ won't change anything
  LocalRootProject / crossSbtVersions := List("1.0.0"),

  scalaVersion := "2.12.12",

  scalacOptions ++= Seq("-encoding", "utf8"),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint"),
  scalacOptions  += "-Xfuture",
  scalacOptions  += "-Yno-adapted-args",
  scalacOptions  += "-Ywarn-dead-code",
  scalacOptions  += "-Ywarn-numeric-widen",
  scalacOptions  += "-Ywarn-value-discard",

  Test /              fork := false,
  Test /       logBuffered := false,
  Test / parallelExecution := true,
))

val dynver = project.settings(
  libraryDependencies += "org.eclipse.jgit"  % "org.eclipse.jgit" % "5.10.0.202012080955-r" % Test,
  libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.15.2"                % Test,

  publishSettings,
)

val sbtdynver = project.dependsOn(dynverP).enablePlugins(SbtPlugin).settings(
  name := "sbt-dynver",

  scriptedLaunchOpts  ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value),
  scriptedBufferLog    := true,
  scriptedDependencies := Seq(dynver / publishLocal, publishLocal).dependOn.value,

  publishSettings,
)

lazy val publishSettings = Def.settings(
  mimaSettings,
  bintrayPackage    := "sbt-dynver",  // keep publishing to the same place
  bintrayRepository := "sbt-plugins",
)

import com.typesafe.tools.mima.core._, ProblemFilters._
lazy val mimaSettings = Seq(
  mimaPreviousArtifacts   := Set(projID.value.withRevision("5.0.0-M2")),
  mimaBinaryIssueFilters ++= Seq(
  ),
)

lazy val projID = Def.setting {
  // Using projectID something is wrong... Looks for dynver_2.12 but artifacts are name=dynver
  val sbtBv = (pluginCrossBuild /   sbtBinaryVersion).value
  val sbv   = (pluginCrossBuild / scalaBinaryVersion).value
  val mid   = organization.value %% moduleName.value % "0.0.0"
  if (sbtPlugin.value) Defaults.sbtPluginExtra(mid, sbtBv, sbv) else mid
}

mimaPreviousArtifacts := Set.empty
publish / skip        := true

Global / excludeLintKeys += crossSbtVersions // Used by the "^" command (PluginCrossCommand)
Global / cancelable      := true
