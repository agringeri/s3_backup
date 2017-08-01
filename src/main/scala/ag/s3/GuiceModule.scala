package ag.s3

import com.google.inject.Provides
import com.google.inject.Singleton
import net.codingwell.scalaguice.ScalaModule
import net.model3.guice.bootstrap.ConfigurationDirectory
import m3.predef._
import m3.fs._
import m3.json.{JsonAssist, Serialization}

class GuiceModule extends ScalaModule with Logging {

  override def configure(): Unit = {
  }

  @Provides @Singleton
  def configurationDirectory: ConfigurationDirectory = {
    val directories =
      List("config", ".")
    .map(d => new ConfigurationDirectory(dir(d)))

    val result: ConfigurationDirectory =
      directories
        .find(d=>d.get.file("config.json").exists)
    .toList
      .headOption match {
      case Some(d) => d
      case None => directories.head
    }

    logger.info(s"using ${result}")

    result
  }

  @Provides @Singleton
  def baseConfig(cd: ConfigurationDirectory): Config = {
    import m3.json.JsonAssist.{ logger => _, _}

    implicit val serializer = Serialization.simpleJsonSerializer

    val f = cd.get.file("config.json")
    val jsonStr = f.readText
    logger.debug(s"loading ${classOf[Config]} from ${f.canonicalPath} -- \n${jsonStr}")
    if ( f.exists )
      parseHocon(jsonStr).deserialize[Config]
    else
      m3x.error(s"unable to find config file ${f.canonicalPath}")
  }

}
