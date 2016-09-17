val sbtdynver = project in file(".")

organization := "com.dwijnand"
        name := "sbt-dynver"
     version := "1.0.0-SNAPSHOT"
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
             test         := { (test in Test).value ; scripted.toTask("").value }

scriptedSettings
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
scriptedBufferLog := false

cancelable in Global := true
