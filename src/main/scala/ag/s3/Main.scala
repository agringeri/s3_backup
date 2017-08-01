package ag.s3

import m3.guice.DaemonApp
import m3.predef._
import net.model3.servlet.runner.JettyRunner

object Main extends DaemonApp {

  def run() = {
    val message = inject[Config].startMessage
    logger.debug(s"startMessage=$message")

    JettyRunner.main(args.toArray)
  }

}
