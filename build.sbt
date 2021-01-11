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

  mimaSettings,
)

import com.typesafe.tools.mima.core._, ProblemFilters._
val sbtdynver = project.dependsOn(dynverP).enablePlugins(SbtPlugin).settings(
  name := "sbt-dynver",

  scriptedLaunchOpts  ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value),
  scriptedBufferLog    := true,
  scriptedDependencies := Seq(dynver / publishLocal, publishLocal).dependOn.value,

  mimaSettings,
)

lazy val mimaSettings = Seq(
  mimaPreviousArtifacts   := Set.empty, // Set(projectID.value.withRevision("4.0.0")),
  mimaBinaryIssueFilters ++= Seq(
    // Migrated from a task key to a setting key
    exclude[IncompatibleResultTypeProblem]("sbtdynver.DynVerPlugin#autoImport.isVersionStable"),
    // private[sbtdynver]
    exclude[DirectMissingMethodProblem]("sbtdynver.GitDescribeOutput.parse"),
    // Migrated from a task key to an initialise
    exclude[IncompatibleResultTypeProblem]("sbtdynver.DynVerPlugin#autoImport.dynverAssertTagVersion"),
    // GitDescribeOutput#Parser is private[sbtdynver]
    exclude[Problem]("sbtdynver.GitDescribeOutput#Parser*"),
    // lightbend/mima#388
    // static method requires()sbt.Plugins in class sbtdynver.DynVerPlugin does not have a correspondent in current version
    exclude[DirectMissingMethodProblem]("sbtdynver.DynVerPlugin.requires"),
  ),
)

mimaPreviousArtifacts := Set.empty
publish / skip := true

Global / cancelable := true

Global / excludeLintKeys += crossSbtVersions // Used by the "^" command (PluginCrossCommand)
