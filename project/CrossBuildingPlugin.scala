import sbt._, Keys._

sealed trait SbtVersionSeries
case object Sbt013 extends SbtVersionSeries
case object Sbt1   extends SbtVersionSeries

object CrossBuildingPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  object autoImport {
    val sbtCrossVersion = sbtVersion in pluginCrossBuild
    val sbtPartV = settingKey[Option[(Long, Long)]]("")
    val sbtVersionSeries = settingKey[SbtVersionSeries]("")
  }
  import autoImport._

  override def globalSettings = Seq(
    sbtPartV := CrossVersion partialVersion sbtCrossVersion.value,
    sbtVersionSeries := (sbtPartV.value match {
      case Some((0, 13)) => Sbt013
      case Some((1, _))  => Sbt1
      case _             => sys error s"Unhandled sbt version ${sbtCrossVersion.value}"
    })
  )
}
