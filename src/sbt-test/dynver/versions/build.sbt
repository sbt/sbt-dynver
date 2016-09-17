def tstamp = Def.setting("%1$tY%1$tm%1$td" format dynverCurrentDate.value)
def headSha = "git rev-parse --short=8 HEAD".!!.init

def check(a: String, e: String) = assert(a == e, s"Version mismatch: Expected $e, Incoming $a")

TaskKey[Unit]("checkNotAGitRepo")         := check(version.value, s"HEAD+${tstamp.value}")
TaskKey[Unit]("checkNoCommits")           := check(version.value, s"HEAD+${tstamp.value}")
TaskKey[Unit]("checkOnCommit")            := check(version.value, s"$headSha")
TaskKey[Unit]("checkOnCommitDirty")       := check(version.value, s"$headSha+${tstamp.value}")
TaskKey[Unit]("checkOnTag")               := check(version.value, s"1.0.0")
TaskKey[Unit]("checkOnTagDirty")          := check(version.value, s"1.0.0+${tstamp.value}")
TaskKey[Unit]("checkOnTagAndCommit")      := check(version.value, s"1.0.0+1-$headSha")
TaskKey[Unit]("checkOnTagAndCommitDirty") := check(version.value, s"1.0.0+1-$headSha+${tstamp.value}")

TaskKey[Unit]("dirty") := {
  import java.nio.file._, StandardOpenOption._
  import scala.collection.JavaConverters._
  Files.write(baseDirectory.value.toPath.resolve("f.txt"), Seq("1").asJava, CREATE, APPEND)
}
