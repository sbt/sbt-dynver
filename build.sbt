val dynverRoot = project.in(file("."))
aggregateProjects(dynverLib, sbtdynver)

inThisBuild(List(
  organization := "com.dwijnand",
      licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
   description := "An sbt plugin to dynamically set your version from git",
    developers := List(Developer("dwijnand", "Dale Wijnand", "dale wijnand gmail com", url("https://dwijnand.com"))),
     startYear := Some(2016),
      homepage := scmInfo.value map (_.browseUrl),
       scmInfo := Some(ScmInfo(url("https://github.com/dwijnand/sbt-dynver"), "scm:git:git@github.com:dwijnand/sbt-dynver.git")),

            Global /      sbtVersion  := "1.1.0", // must be Global, otherwise ^^ won't change anything
  LocalRootProject / crossSbtVersions := List("1.1.0"),

  scalaVersion := "2.12.17",

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

val dynverLib = LocalProject("dynver")
val dynver    = project.settings(
  libraryDependencies += "org.eclipse.jgit"  % "org.eclipse.jgit" % "5.12.0.202106070339-r" % Test,
  libraryDependencies += "org.scalacheck"   %% "scalacheck"       % "1.15.4"                % Test,
  resolvers           += Resolver.sbtPluginRepo("releases"), // for prev artifacts, not repo1 b/c of mergly publishing
  publishSettings,
  publishMavenStyle   := false, // so it's resolved out of sbt-plugin-releases as a dep of sbt-dynver
)

val sbtdynver = project.dependsOn(dynverLib).enablePlugins(SbtPlugin).settings(
                  name := "sbt-dynver",
  scriptedBufferLog    := true,
  scriptedDependencies := Seq(dynver / publishLocal, publishLocal).dependOn.value,
  scriptedLaunchOpts   += s"-Dplugin.version=${version.value}",
  scriptedLaunchOpts   += s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}",
  publishSettings,
)

lazy val publishSettings = Def.settings(
  MimaSettings.mimaSettings,
  bintrayPackage      := "sbt-dynver",  // keep publishing to the same place
  bintrayRepository   := "sbt-plugins",
  bintray / resolvers := Nil, // disable getting my bintray repo through my local credentials; be like CI
)

mimaPreviousArtifacts := Set.empty
publish / skip        := true

Global / excludeLintKeys += crossSbtVersions // Used by the "^" command (PluginCrossCommand)
Global / cancelable      := true
