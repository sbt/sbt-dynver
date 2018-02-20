import scala.sys.process.stringToProcess

lazy val foo = project in file("foo")
lazy val bar = project in file("bar")

def tstamp = Def.setting(sbtdynver.DynVer timestamp dynverCurrentDate.value)
def headSha = {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  Def.task("git rev-parse --short=8 HEAD".!!(streams.value.log).trim)
}

def check(a: String, e: String) = assert(a == e, s"Version mismatch: Expected $e, Incoming $a")

TaskKey[Unit]("checkOnTagFoo") := check((version in foo).value, "1.0.0")
TaskKey[Unit]("checkOnTagBar") := check((version in bar).value, "2.0.0")
TaskKey[Unit]("checkOnTagBarDirty")          := check((version in bar).value, s"2.0.0+${tstamp.value}")
TaskKey[Unit]("checkOnTagBarAndCommit")      := check((version in bar).value, s"2.0.0+1-${headSha.value}")
TaskKey[Unit]("checkOnTagBarAndCommitDirty") := check((version in bar).value, s"2.0.0+1-${headSha.value}+${tstamp.value}")

import sbt.complete.DefaultParsers._

def exec(cmd: String, streams: TaskStreams): String = {
  import scala.sys.process._
  implicit def log2log(log: Logger): ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  cmd !! streams.log
}

val dirParser = Def setting fileParser(baseDirectory.value)
val tagParser = Def setting (Space ~> StringBasic)

InputKey[Unit]("gitInitSetup") := {
  exec("git init .", streams.value)
  exec("git config user.email dynver@mailinator.com", streams.value)
  exec("git config user.name dynver", streams.value)
  IO.writeLines(baseDirectory.value / ".gitignore", Seq("target/"))
}
InputKey[Unit]("gitAdd")    := exec("git add .", streams.value)
InputKey[Unit]("gitCommit") := exec("git commit -am1", streams.value)
InputKey[Unit]("gitTag")    := {
  val tag = tagParser.value.parsed
  exec(s"git tag -a $tag -m '$tag'", streams.value)
}

InputKey[Unit]("dirty") := {
  import java.nio.file._, StandardOpenOption._
  import scala.collection.JavaConverters._
  Files.write(dirParser.value.parsed.toPath resolve "f.txt", Seq("1").asJava, CREATE, APPEND)
}

def sbtLoggerToScalaSysProcessLogger(log: Logger): scala.sys.process.ProcessLogger =
  new scala.sys.process.ProcessLogger {
    def buffer[T](f: => T): T   = f
    def err(s: => String): Unit = log info s
    def out(s: => String): Unit = log error s
  }
