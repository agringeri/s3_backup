package ag.s3

import m3.predef._

object DemoUploadFileDirectoryFull extends App with Logging {

  val config: Config = inject[Config]

  // Upload file (oldest file will also be removed)
  /*
  S3Manager.upload(
    config.bucketName, // bucketName
    "test/fullDirectory_Hourly/", // location
    s"test_file${scala.util.Random.nextInt(1000)}.txt", // fileName
    TestingTools.createSampleFile // file
  )
  */

}
