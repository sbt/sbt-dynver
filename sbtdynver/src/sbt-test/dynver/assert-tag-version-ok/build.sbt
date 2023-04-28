import scala.sys.process.stringToProcess
import scala.sys.process.ProcessLogger

TaskKey[Unit]("gitInitSetup") := {
  implicit def log2log(log: Logger): ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git init".!!(streams.value.log)
  "git config user.email dynver@mailinator.com".!!(streams.value.log)
  "git config user.name dynver".!!(streams.value.log)
}

TaskKey[Unit]("gitAdd")    := {
  implicit def log2log(log: Logger): ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git add .".!!(streams.value.log)
}
TaskKey[Unit]("gitCommit") := {
  implicit def log2log(log: Logger): ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git commit -am1".!!(streams.value.log)
}
TaskKey[Unit]("gitTag")    := {
  implicit def log2log(log: Logger): ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git tag -a v1.0.0 -m1.0.0".!!(streams.value.log)
}
def sbtLoggerToScalaSysProcessLogger(log: Logger): ProcessLogger =
  new ProcessLogger {
    def buffer[T](f: => T): T   = f
    def err(s: => String): Unit = log info s
    def out(s: => String): Unit = log error s
  }
