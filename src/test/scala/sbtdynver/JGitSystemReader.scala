package sbtdynver

import java.io.{ File, IOException }
import java.net.{ InetAddress, UnknownHostException }
import java.nio.file.{ Files, InvalidPathException, Path, Paths }

import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.{ Config, Constants }
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.{ FS, StringUtils, SystemReader }
import org.slf4j.LoggerFactory

// Copy of org.eclipse.jgit.util.SystemReader.Default with:
// * calls to Files.createDirectories guarded by if !Files.isDirectory
//   necessary because my ~/.config is a symlink to a directory
//   which Files.createDirectories isn't happy with
object JGitSystemReader extends SystemReader {
  private val LOG = LoggerFactory.getLogger(getClass)

  lazy val init: Unit = SystemReader.setInstance(this)

  override lazy val getHostname = {
    try InetAddress.getLocalHost.getCanonicalHostName
    catch { case _: UnknownHostException => "localhost" }
  }.ensuring(_ != null)

  override def getenv(variable: String): String = System.getenv(variable)
  override def getProperty(key: String): String = System.getProperty(key)
  override def getCurrentTime: Long             = System.currentTimeMillis
  override def getTimezone(when: Long): Int     = getTimeZone.getOffset(when) / (60 * 1000)

  override def openUserConfig(parent: Config, fs: FS) =
    new FileBasedConfig(parent, new File(fs.userHome, ".gitconfig"), fs)

  override def openSystemConfig(parent: Config, fs: FS): FileBasedConfig = {
    if (StringUtils.isEmptyOrNull(getenv(Constants.GIT_CONFIG_NOSYSTEM_KEY))) {
      val configFile = fs.getGitSystemConfig
      if (configFile != null) return new FileBasedConfig(parent, configFile, fs)
    }
    new FileBasedConfig(parent, null, fs) {
      override def load(): Unit = () // do not load
      override def isOutdated   = false // regular class would bomb here
    }
  }

  override def openJGitConfig(parent: Config, fs: FS): FileBasedConfig = {
    val xdgPath = getXDGConfigHome(fs)
    if (xdgPath != null) {
      var configPath: Path = null
      try {
        configPath = xdgPath.resolve("jgit")
        if (!Files.isDirectory(configPath))
          Files.createDirectories(configPath)
        configPath = configPath.resolve(Constants.CONFIG)
        return new FileBasedConfig(parent, configPath.toFile, fs)
      } catch {
        case e: IOException =>
          LOG.error(JGitText.get.createJGitConfigFailed, configPath: Any, e)
      }
    }
    new FileBasedConfig(parent, new File(fs.userHome, ".jgitconfig"), fs)
  }

  private def getXDGConfigHome(fs: FS): Path = {
    var configHomePath = getenv(Constants.XDG_CONFIG_HOME)
    if (StringUtils.isEmptyOrNull(configHomePath))
      configHomePath = new File(fs.userHome, ".config").getAbsolutePath
    try {
      val xdgHomePath = Paths.get(configHomePath)
      if (!Files.isDirectory(xdgHomePath))
        Files.createDirectories(xdgHomePath)
      xdgHomePath
    } catch {
      case e @ (_: IOException | _: InvalidPathException) =>
        LOG.error(JGitText.get.createXDGConfigHomeFailed, configHomePath: Any, e)
        null
    }
  }
}

