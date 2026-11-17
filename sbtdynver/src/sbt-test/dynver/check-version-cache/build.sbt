import scala.sys.process.Process

@transient
val initialiseRepository = taskKey[Unit]("Create a clean tagged repository")
@transient
val checkMatchingVersion = taskKey[Unit]("Check the dynamic version still matches")
@transient
val checkChangedVersion = taskKey[Unit]("Check a working-tree change is observed")

initialiseRepository := {
  val base = baseDirectory.value
  IO.write(base / ".gitignore", "target/\n.bsp/\nproject/project/\n")
  IO.copyFile(base / "clean-marker.txt", base / "marker.txt")
  def git(args: String*): Unit = {
    assert(Process(Seq("git") ++ args, base).! == 0, "git " + args.mkString(" "))
  }
  git("init", "--initial-branch=main")
  git("add", ".")
  git("-c", "user.name=Dynver Test", "-c", "user.email=dynver@example.invalid", "commit", "-m", "Initial")
  git("tag", "v1.0.0")
}

checkMatchingVersion := assert(dynverCheckVersion.value, "Expected the clean tagged version to match")
checkChangedVersion := assert(!dynverCheckVersion.value, "Expected the dirty working tree to change dynver")
