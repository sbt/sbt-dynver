import scala.sys.process.stringToProcess

ThisBuild / dynverSeparator := "-"

def tstamp = Def.setting(sbtdynver.DynVer timestamp dynverCurrentDate.value)
def headSha = {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  Def.task("git rev-parse --short=8 HEAD".!!(streams.value.log).trim)
}

def check(a: String, e: String) = assert(a == e, s"Version mismatch: Expected $e, Incoming $a")

TaskKey[Unit]("checkNotAGitRepo")         := check(version.value, s"HEAD-${tstamp.value}")
TaskKey[Unit]("checkNoCommits")           := check(version.value, s"HEAD-${tstamp.value}")
TaskKey[Unit]("checkOnCommit")            := check(version.value, s"0.0.0-1-${headSha.value}")
TaskKey[Unit]("checkOnCommitDirty")       := check(version.value, s"0.0.0-1-${headSha.value}-${tstamp.value}")
TaskKey[Unit]("checkOnTag")               := check(version.value, s"1.0.0")
TaskKey[Unit]("checkOnTagDirty")          := check(version.value, s"1.0.0-0-${headSha.value}-${tstamp.value}")
TaskKey[Unit]("checkOnTagAndCommit")      := check(version.value, s"1.0.0-1-${headSha.value}")
TaskKey[Unit]("checkOnTagAndCommitDirty") := check(version.value, s"1.0.0-1-${headSha.value}-${tstamp.value}")

TaskKey[Unit]("gitInitSetup") := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git init".!!(streams.value.log)
  "git config user.email dynver@mailinator.com".!!(streams.value.log)
  "git config user.name dynver".!!(streams.value.log)
}

TaskKey[Unit]("gitAdd")    := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git add .".!!(streams.value.log)
}
TaskKey[Unit]("gitCommit") := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git commit -am1".!!(streams.value.log)
}
TaskKey[Unit]("gitTag")    := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git tag -a v1.0.0 -m1.0.0".!!(streams.value.log)
}

TaskKey[Unit]("dirty") := {
  import java.nio.file._, StandardOpenOption._
  import scala.collection.JavaConverters._
  Files.write(baseDirectory.value.toPath.resolve("f.txt"), Seq("1").asJava, CREATE, APPEND)
}

def sbtLoggerToScalaSysProcessLogger(log: Logger): scala.sys.process.ProcessLogger =
  new scala.sys.process.ProcessLogger {
    def buffer[T](f: => T): T   = f
    def err(s: => String): Unit = log info s
    def out(s: => String): Unit = log error s
  }
