lazy val dynverRoot = project.in(file("."))
  .aggregate(dynverLib, sbtdynver)
  .settings(
    crossScalaVersions := Nil,
    mimaPreviousArtifacts := Set.empty,
    publish / skip := true,
  )

lazy val scala2_12 = "2.12.21"
lazy val scala2_13 = "2.13.18"
lazy val scala3    = "3.3.8"
lazy val scala3sbt = "3.8.4"

def compilerOptions(scalaBinaryVersion: String): Seq[String] =
  val shared = Seq(
    "-encoding", "utf8",
    "-deprecation",
    "-feature",
    "-unchecked",
  )
  scalaBinaryVersion match {
    case "2.12" => shared ++ Seq(
        "-release:8",
        "-Xlint",
        "-Xfuture",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
        "-Ywarn-value-discard",
        "-Yno-adapted-args",
      )
    case _ => shared
  }

inThisBuild(List(
             scalaVersion := scala2_12,
             organization := "com.github.sbt",
                 licenses := Seq("Apache-2.0" -> uri("https://www.apache.org/licenses/LICENSE-2.0")),
              description := "An sbt plugin to dynamically set your version from git",
               developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", uri("https://dwijnand.com"))),
                startYear := Some(2016),
                 homepage := scmInfo.value.map(_.browseUrl),
                  scmInfo := Some(ScmInfo(uri("https://github.com/sbt/sbt-dynver"), "scm:git:git@github.com:sbt/sbt-dynver.git")),
  dynverSonatypeSnapshots := true,
  Test /              fork := false,
  Test /       logBuffered := false,
  Test / parallelExecution := true,
))

lazy val dynverLib = LocalProject("dynver")
lazy val dynver    = project.settings(
  libraryDependencies += "org.eclipse.jgit"  % "org.eclipse.jgit" % "5.13.3.202401111512-r" % Test,
  libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.19.0"                % Test,
  publishSettings,
  crossScalaVersions := Seq(scala2_12, scala2_13, scala3),
  scalacOptions ++= compilerOptions(scalaBinaryVersion.value),
  scripted := (()),
)

val sbtdynver = project.dependsOn(dynverLib).enablePlugins(SbtPlugin).settings(
  name                 := "sbt-dynver",
  scriptedBufferLog    := true,
  scriptedDependencies := Def.task(()).dependsOn(dynver / publishLocal, publishLocal).value,
  scriptedLaunchOpts   += s"-Dplugin.version=${version.value}",
  scriptedLaunchOpts   += s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}",
  crossScalaVersions   := Seq(scala2_12, scala3sbt),
  (pluginCrossBuild / sbtVersion) := {
    scalaBinaryVersion.value match {
      case "2.12" => "1.12.3"
      case _ => "2.0.2"
    }
  },
  scalacOptions ++= compilerOptions(scalaBinaryVersion.value),
  publishSettings,
)

lazy val publishSettings = Def.settings(
  MimaSettings.mimaSettings,
)

Global / cancelable := true
