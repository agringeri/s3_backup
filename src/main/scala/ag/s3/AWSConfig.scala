package ag.s3

import com.google.inject.{Inject, Singleton}

@Singleton
case class AWSConfig @Inject()(
  accessKey: String,
  secretKey: String,
  bucketName: String,
  region: String,
)