val sbtdynver = project.in(file(".")).settings(name := "sbt-dynver")

organization := "com.dwijnand"
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
 description := "An sbt plugin to dynamically set your version from git"
  developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", url("https://dwijnand.com")))
   startYear := Some(2016)
    homepage := scmInfo.value map (_.browseUrl)
     scmInfo := Some(ScmInfo(url("https://github.com/dwijnand/sbt-dynver"), "scm:git:git@github.com:dwijnand/sbt-dynver.git"))

enablePlugins(SbtPlugin)
Global / sbtVersion  := "1.0.0" // must be Global, otherwise ^^ won't change anything
    crossSbtVersions := List("1.0.0")

scalaVersion := "2.12.12"

       maxErrors := 15
triggeredMessage := Watched.clearWhenTriggered

scalacOptions ++= Seq("-encoding", "utf8")
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")
scalacOptions  += "-Xfuture"
scalacOptions  += "-Yno-adapted-args"
scalacOptions  += "-Ywarn-dead-code"
scalacOptions  += "-Ywarn-numeric-widen"
scalacOptions  += "-Ywarn-value-discard"

libraryDependencies += "org.eclipse.jgit"  % "org.eclipse.jgit" % "5.9.0.202009080501-r" % Test
libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.15.2"               % Test

Test /              fork := false
Test /       logBuffered := false
Test / parallelExecution := true

scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
scriptedBufferLog := true

def toSbtPlugin(m: ModuleID) = Def.setting(
  Defaults.sbtPluginExtra(m, (pluginCrossBuild / sbtBinaryVersion).value, (update / scalaBinaryVersion).value)
)

mimaPreviousArtifacts := Set(toSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0").value)

import com.typesafe.tools.mima.core._, ProblemFilters._
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
)

Global / cancelable := true
