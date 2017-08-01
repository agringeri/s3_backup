package ag.s3

import com.google.inject.{Inject, Singleton}

@Singleton
case class Config @Inject()(
  accessKey: String,
  secretKey: String,
)
