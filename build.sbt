val dynverRoot = project.in(file("."))
aggregateProjects(dynverLib, sbtdynver)

lazy val scala2_12 = "2.12.18"
lazy val scala2_13 = "2.13.13"
lazy val scala3    = "3.3.3"
lazy val scalacOptions212 = Seq(
  "-Xlint",
  "-Xfuture",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Yno-adapted-args",
)

inThisBuild(List(
             scalaVersion := scala2_12,
             organization := "com.github.sbt",
                 licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
              description := "An sbt plugin to dynamically set your version from git",
               developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", url("https://dwijnand.com"))),
                startYear := Some(2016),
                 homepage := scmInfo.value map (_.browseUrl),
                  scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbt-dynver"), "scm:git:git@github.com:sbt/sbt-dynver.git")),
  dynverSonatypeSnapshots := true,
                  version := {
                    val orig = version.value
                    if (orig.endsWith("-SNAPSHOT")) "5.0.1-SNAPSHOT"
                    else orig
                  },

  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-deprecation",
    "-feature",
    "-unchecked",
  ) ++ scalacOptions212,
  Test /              fork := false,
  Test /       logBuffered := false,
  Test / parallelExecution := true,
))

val dynverLib = LocalProject("dynver")
val dynver    = project.settings(
  libraryDependencies += "org.eclipse.jgit"  % "org.eclipse.jgit" % "5.13.3.202401111512-r" % Test,
  libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.17.0"                % Test,
  publishSettings,
  crossScalaVersions ++= Seq(scala2_13, scala3),
  scalacOptions := {
    scalaBinaryVersion.value match {
      case "3" | "2.13" => scalacOptions.value.filterNot(scalacOptions212.contains(_))
      case _            => scalacOptions.value
    }
  }
)

val sbtdynver = project.dependsOn(dynverLib).enablePlugins(SbtPlugin).settings(
                  name := "sbt-dynver",
  scriptedBufferLog    := true,
  scriptedDependencies := Seq(dynver / publishLocal, publishLocal).dependOn.value,
  scriptedLaunchOpts   += s"-Dplugin.version=${version.value}",
  scriptedLaunchOpts   += s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}",
  (pluginCrossBuild / sbtVersion) := {
    scalaBinaryVersion.value match {
      case "2.12" => "1.3.0"
    }
  },
  publishSettings,
)

lazy val publishSettings = Def.settings(
  MimaSettings.mimaSettings,
)

mimaPreviousArtifacts := Set.empty
publish / skip        := true
Global / cancelable      := true
