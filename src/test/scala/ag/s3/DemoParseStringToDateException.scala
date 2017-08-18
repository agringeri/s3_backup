package ag.s3

import m3.predef._

object DemoParseStringToDateException extends App with Logging {

  val config: Config = inject[Config]

  //S3Manager.parseStringToDate("this string is not formatted correctly - trying to parse it will throw an exception")

}