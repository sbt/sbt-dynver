dependsOn(RootProject(file("bar")))

def check(a: String, e: String) = assert(a == e, s"Version mismatch: Expected $e, Incoming $a")

TaskKey[Unit]("checkOnTagFoo") := check(version.value, "1.0.0")
TaskKey[Unit]("checkOnTagBar") := check((version in RootProject(file("bar"))).value, "2.0.0")

import sbt.complete.DefaultParsers._

def exec(cmd: String, dir: File, streams: TaskStreams): String = {
  import scala.sys.process._
  implicit def log2log(log: Logger): ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  Process(cmd, Some(dir)) !! streams.log
}

val dirParser = Def setting fileParser(baseDirectory.value)
val dirAndTagParser = Def setting (dirParser.value ~ (Space ~> StringBasic))

InputKey[Unit]("gitInitSetup") := {
  val dir = dirParser.value.parsed
  exec("git init", dir, streams.value)
  exec("git config user.email dynver@mailinator.com", dir, streams.value)
  exec("git config user.name dynver", dir, streams.value)
  IO.writeLines(dir / ".gitignore", Seq("target/"))
}

InputKey[Unit]("gitAdd")    := exec("git add .", dirParser.value.parsed, streams.value)
InputKey[Unit]("gitCommit") := exec("git commit -am1", dirParser.value.parsed, streams.value)
InputKey[Unit]("gitTag")    := {
  val (dir, tag) = dirAndTagParser.value.parsed
  exec(s"git tag -a v$tag -m '$tag'", dir, streams.value)
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
