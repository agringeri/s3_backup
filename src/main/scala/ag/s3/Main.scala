package ag.s3

import m3.guice.DaemonApp
import net.model3.servlet.runner.JettyRunner

object Main extends DaemonApp {

  def run() = {
    JettyRunner.main(args.toArray)
  }

}
