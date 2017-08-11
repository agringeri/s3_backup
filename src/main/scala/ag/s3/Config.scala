package ag.s3

import com.google.inject.{Inject, Singleton}

@Singleton
case class Config @Inject()(
  accessKey: String,
  secretKey: String,
  bucketName: String,
  s3BackupLimits: S3BackupLimits,
  glacierBackupLimits: GlacierBackupLimits,
)

case class S3BackupLimits(
                           hourly: Int,
                           daily: Int,
                           monthly: Int,
                         )

case class GlacierBackupLimits(
                                hourly: Int,
                                daily: Int,
                                monthly: Int,
                              )
