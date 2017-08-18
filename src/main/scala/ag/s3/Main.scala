package ag.s3

import m3.guice._
import m3.predef._
import net.model3.servlet.runner.JettyRunner
import java.io.File
import java.time._
import java.time.format._
import java.util._

import m3.json.Handlers.dateTime

object Main extends ForegroundApp {

  def run() = {

    logger.info(s"args = ${args}")

    // Check that an argument was passed
    if (args.size < 1) {
      logger.warn("Failed to pass in file argument. Pass in a filepath as the first argument where filepath is the absolute path to the file")
      System.exit(1)
    }

    // Create file from argument
    var file = new File(args(0).toLowerCase) // S3 Files are case sensitive - keep it underscore

    // Get filetype (extension) from filename
    val fileType =
      file.getName.splitList("\\.", 2) match {
        case scala.collection.immutable.List(f) => ""
        case scala.collection.immutable.List(p, s) => s".${s}"
      }

    // If the file path is not valid
    if (!file.isFile) {
      logger.warn(
        "The file path that was provided is not valid - the path is not a file\n" +
          "Enter a valid absolute path to a file"
      )
      System.exit(1)
    }

    // Get the type of backup (gitlab, postgres, etc) from filename
    val backupType =
      file.getName.splitList("_", 2) match {
        case scala.collection.immutable.List(f) => ""
        case scala.collection.immutable.List(p, s) => p
      }

    // If the format of the filename is not as expected
    if (!BackupCategory.values.map(v => (v.toString, v)).toMap.contains(backupType)) {
      logger.warn("The file provided does not meet formatting required for uploading")
      logger.warn("A file being uploaded must have a prefix that is defined in BackupCategory followed by an underscore")
      logger.warn("For example, a gitlab backup should have a filename such as \"gitlab_sampleFileName_abcd.xyz\"")
      System.exit(1)
    }

    // Get timestamp in correct format
    val dateTime = LocalDateTime.now()
    val timeStampForFileName =
      dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmmss"))

    // Standardized file name for file to-be-uploaded
    val newFileName =
      s"${backupType}_${timeStampForFileName}${fileType}"

    logger.debug(newFileName)

    // Attempt upload
    S3Manager.uploadBackup(
      BackupCategory.withName(backupType),
      newFileName,
      file
    )

  }

}
