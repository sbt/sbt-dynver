import scala.sys.process.stringToProcess

def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val dirtySuffix = out.dirtySuffix.dropPlus.mkString("-", "")
  if (out.isCleanAfterTag) out.ref.dropV.value + dirtySuffix // no commit info if clean after tag
  else out.ref.dropV.value + out.commitSuffix.mkString("-", "-", "") + dirtySuffix
}

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer timestamp d}"

version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value))
dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
}

def tstamp = Def.setting(sbtdynver.DynVer timestamp dynverCurrentDate.value)
def headSha = {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  Def.task("git rev-parse --short=8 HEAD".!!(streams.value.log).trim)
}

def check(a: String, e: String) = assert(a == e, s"Version mismatch: Expected $e, Incoming $a")

@transient
lazy val checkNotAGitRepo         = taskKey[Unit]("")
@transient
lazy val checkNoCommits           = taskKey[Unit]("")
@transient
lazy val checkOnCommit            = taskKey[Unit]("")
@transient
lazy val checkOnCommitDirty       = taskKey[Unit]("")
@transient
lazy val checkOnTag               = taskKey[Unit]("")
@transient
lazy val checkOnTagDirty          = taskKey[Unit]("")
@transient
lazy val checkOnTagAndCommit      = taskKey[Unit]("")
@transient
lazy val checkOnTagAndCommitDirty = taskKey[Unit]("")
@transient
lazy val gitInitSetup             = taskKey[Unit]("")
@transient
lazy val gitAdd                   = taskKey[Unit]("")
@transient
lazy val gitCommit                = taskKey[Unit]("")
@transient
lazy val gitTag                   = taskKey[Unit]("")
@transient
lazy val dirty                    = taskKey[Unit]("")

checkNotAGitRepo         := check(version.value, s"HEAD-${tstamp.value}")
checkNoCommits           := check(version.value, s"HEAD-${tstamp.value}")
checkOnCommit            := check(version.value, s"0.0.0-1-${headSha.value}")
checkOnCommitDirty       := check(version.value, s"0.0.0-1-${headSha.value}-${tstamp.value}")
checkOnTag               := check(version.value, s"1.0.0")
checkOnTagDirty          := check(version.value, s"1.0.0-0-${headSha.value}-${tstamp.value}")
checkOnTagAndCommit      := check(version.value, s"1.0.0-1-${headSha.value}")
checkOnTagAndCommitDirty := check(version.value, s"1.0.0-1-${headSha.value}-${tstamp.value}")

gitInitSetup := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  IO.writeLines(baseDirectory.value / ".gitignore", List("target", ".bsp"))
  "git config --global init.defaultBranch main".!!(streams.value.log)
  "git init".!!(streams.value.log)
  "git config user.email dynver@mailinator.com".!!(streams.value.log)
  "git config user.name dynver".!!(streams.value.log)
}

gitAdd := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git add .".!!(streams.value.log)
}
gitCommit := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git commit -am1".!!(streams.value.log)
}
gitTag := {
  implicit def log2log(log: Logger): scala.sys.process.ProcessLogger = sbtLoggerToScalaSysProcessLogger(log)
  "git tag -a v1.0.0 -m1.0.0".!!(streams.value.log)
}

dirty := {
  import java.nio.file._, StandardOpenOption._
  import scala.collection.JavaConverters._
  Files.write(baseDirectory.value.toPath.resolve("f.txt"), Seq("1").asJava, CREATE, APPEND)
}

def sbtLoggerToScalaSysProcessLogger(log: Logger): scala.sys.process.ProcessLogger =
  new scala.sys.process.ProcessLogger {
    def buffer[T](f: => T): T   = f
    def err(s: => String): Unit = log.info(s)
    def out(s: => String): Unit = log.error(s)
  }
