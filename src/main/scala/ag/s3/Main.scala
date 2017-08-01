package ag.s3

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3._
import m3.guice.DaemonApp
import m3.predef._
import net.model3.servlet.runner.JettyRunner
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.S3ClientOptions.Builder

object Main extends DaemonApp {

  def run() = {
    //JettyRunner.main(args.toArray)
  }

  val credentials: BasicAWSCredentials = new BasicAWSCredentials(
    inject[Config].accessKey,
    inject[Config].secretKey,
  )

  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard.withCredentials(
    new AWSStaticCredentialsProvider(credentials)).withRegion("us-east-1").withForceGlobalBucketAccessEnabled(true).build()

  // Send sample request (list objects in a given bucket)
  val objectListing : ObjectListing = s3Client.listObjects(new
      ListObjectsRequest().withBucketName("accur8-backup"))
}
