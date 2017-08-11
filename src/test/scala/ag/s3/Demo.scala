package ag.s3

import m3.predef._
import java.io._

import ag.s3.S3Manager.config
import com.amazonaws.auth._
import com.amazonaws._
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._

object Demo extends App with Logging {

  val config: Config = inject[Config]

  // Fill "/test/fullDirectory_Hourly/" with 24 random files
  /*
  for (i <- 1 to 24) {
    S3Manager.upload(
      config.bucketName,
      "test/fullDirectory_Hourly/",
      s"test_file${scala.util.Random.nextInt(1000)}.txt",
      TestingTools.createSampleFile
    )
  }
  */

}