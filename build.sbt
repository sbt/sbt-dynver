val sbtdynver = project in file(".")

organization := "com.dwijnand"
        name := "sbt-dynver"
    licenses := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
 description := "An sbt plugin to dynamically set your version from git"
  developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", url("https://dwijnand.com")))
   startYear := Some(2016)
    homepage := scmInfo.value map (_.browseUrl)
     scmInfo := Some(ScmInfo(url("https://github.com/dwijnand/sbt-dynver"), "scm:git:git@github.com:dwijnand/sbt-dynver.git"))

   sbtPlugin := true
scalaVersion := "2.10.6"

       maxErrors := 15
triggeredMessage := Watched.clearWhenTriggered

scalacOptions ++= Seq("-encoding", "utf8")
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")
scalacOptions  += "-Xfuture"
scalacOptions  += "-Yno-adapted-args"
scalacOptions  += "-Ywarn-dead-code"
scalacOptions  += "-Ywarn-numeric-widen"
scalacOptions  += "-Ywarn-value-discard"

libraryDependencies += "org.eclipse.jgit"  % "org.eclipse.jgit" % "4.4.1.201607150455-r" % Test
libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.13.2"               % Test

             fork in Test := false
      logBuffered in Test := false
parallelExecution in Test := true

scriptedSettings
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
scriptedBufferLog := true

def toSbtPlugin(m: ModuleID) = Def.setting(
  Defaults.sbtPluginExtra(m, (sbtBinaryVersion in update).value, (scalaBinaryVersion in update).value)
)
import com.typesafe.tools.mima.core._, ProblemFilters._
mimaPreviousArtifacts := Set(toSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.1.1").value)
mimaBinaryIssueFilters ++= Seq(
  exclude[MissingTypesProblem]("sbtdynver.DynVer$"),          // dropped synthetic abstract function parent
  exclude[MissingTypesProblem]("sbtdynver.GitRef$"),          // dropped synthetic abstract function parent
  exclude[MissingTypesProblem]("sbtdynver.GitCommitSuffix$"), // dropped synthetic abstract function parent
  exclude[MissingTypesProblem]("sbtdynver.GitDirtySuffix$"),  // dropped synthetic abstract function parent
  exclude[DirectMissingMethodProblem]("sbtdynver.package.timestamp") // dropped package private method
)

TaskKey[Unit]("verify") := Def.sequential(test in Test, scripted.toTask(""), mimaReportBinaryIssues).value

cancelable in Global := true
