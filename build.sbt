val sbtdynver = project in file(".")

organization := "com.dwijnand"
        name := "sbt-dynver"
    licenses := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
 description := "An sbt plugin to dynamically set your version from git"
  developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", url("https://dwijnand.com")))
   startYear := Some(2016)
    homepage := scmInfo.value map (_.browseUrl)
     scmInfo := Some(ScmInfo(url("https://github.com/dwijnand/sbt-dynver"), "scm:git:git@github.com:dwijnand/sbt-dynver.git"))

       sbtPlugin           := true
      sbtVersion in Global := "0.13.16" // must be Global, otherwise ^^ won't change anything
crossSbtVersions           := List("0.13.16", "1.0.0")

scalaVersion := (sbtVersionSeries.value match { case Sbt013 => "2.10.6"; case Sbt1 => "2.12.4" })

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
libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.13.5"               % Test

             fork in Test := false
      logBuffered in Test := false
parallelExecution in Test := true

scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
scriptedBufferLog := true

def toSbtPlugin(m: ModuleID) = Def.setting(
  Defaults.sbtPluginExtra(m, (sbtBinaryVersion in update).value, (scalaBinaryVersion in update).value)
)
import com.typesafe.tools.mima.core._, ProblemFilters._
mimaPreviousArtifacts := (sbtVersionSeries.value match {
  case Sbt013 => Set(toSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.0.0").value)
  case Sbt1   => Set.empty // TODO
})
mimaBinaryIssueFilters ++= Seq()

// TaskKey[Unit]("verify") := Def.sequential(test in Test, scripted.toTask(""), mimaReportBinaryIssues).value

cancelable in Global := true
