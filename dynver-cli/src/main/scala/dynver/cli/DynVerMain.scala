package dynver
package cli

import java.io.File
import java.util.Date

import scopt.OParser

import sbtdynver._

object DynVerMain {
  def main(args: Array[String]): Unit = {
    val optionBuilder = OParser.builder[Conf]
    import optionBuilder._

    val optionParser = OParser.sequence(
      programName("dynver"),
      head("dynver", BuildInfo.version),
      opt[String]('t', "tag-prefix")
        .action((t, o) => o.copy(tagPrefix = t))
        .text("The prefix to use when matching the version tag"),
      opt[String]('s', "separator")
        .action((s, o) => o.copy(separator = s))
        .text("The separator to use between tag and distance, and the hash and dirty timestamp"),
      opt[Unit]("sonatype-snapshot")
        .action((_, o) => o.copy(sonatypeSnapshots = true))
        .text("Whether to append -SNAPSHOT to snapshot versions"),
      help("help").text("prints this usage text"),
    )

    val conf = OParser.parse(optionParser, args, Conf()).getOrElse(sys.exit(1))

    val dynver  = DynVer(Some(new File(".")), conf.separator, conf.tagPrefix)
    val date    = new Date
    val out     = dynver.getGitDescribeOutput(date)
    val version = out.getVersion(date, conf.separator, conf.sonatypeSnapshots)
    out.assertTagVersion(version)
    println(version)
  }

  private final case class Conf(
      tagPrefix: String          = "v",              // The prefix to use when matching the version tag
      separator: String          = DynVer.separator, // The separator to use between tag and distance, and the hash and dirty timestamp
      sonatypeSnapshots: Boolean = false,            // Whether to append -SNAPSHOT to snapshot versions
  )
}

