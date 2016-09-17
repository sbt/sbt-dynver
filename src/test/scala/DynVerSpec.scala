import java.nio.file._, StandardOpenOption._

import scala.collection.JavaConverters._

import org.scalacheck._, Prop._
import org.eclipse.jgit.api._

import sbtdynver._

object DynVerSpec extends Properties("DynVerSpec") {
  property("when on v1.0.0 tag, w/o local changes") = tagClean()
  property("when on v1.0.0 tag with local changes") = passed
  property("when on commit 1234abcd: 3 commits after v1.0.0 tag, w/o local changes") = passed
  property("when on commit 1234abcd: 3 commits after v1.0.0 tag with local changes") = passed
  property("when there are no tags, on commit 1234abcd, w/o local changes") = passed
  property("when there are no tags, on commit 1234abcd with local changes") = passed
  property("when there are no commits") = passed
  property("when not a git repo") = passed

  def tagClean(): Prop = {
    val dir = Files.createTempDirectory("dynver-test-tag-clean-").toFile
    dir.deleteOnExit()

    val git = Git.init().setDirectory(dir).call()

    val file = dir.toPath.resolve("f.txt")

    Files.write(file, Seq("1").asJava, CREATE, APPEND)

    git.add().addFilepattern(".").call()

    git.commit().setMessage("1").call()

    git.tag().setName("v1.0.0").setAnnotated(true).call()

    val dynver = DynVer(Some(dir), FakeClock(new GregorianCalendar(2016, 9, 17).getTime))

    dynver.version() ?= "1.0.0"
  }
}
