package ag.s3

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import m3.predef._

object Demo extends App with Logging {

  val config: Config = inject[Config]

  //S3Manager.uploadBackup(BackupCategory.gitlab, "test_backup.tar.gz", TestingTools.createSampleFile)

}