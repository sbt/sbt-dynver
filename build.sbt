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

             fork in Test := false
      logBuffered in Test := false
parallelExecution in Test := true

cancelable in Global := true
