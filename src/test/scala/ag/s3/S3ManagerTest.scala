package ag.s3

import java.util._

import m3.predef._
import org.scalatest.FunSpec

class S3ManagerTest extends FunSpec with Logging {

  import m3.test.FunSpecMacros._

  implicit def runner(input:Boolean, expected: Boolean): Unit = {
    val actual = S3Manager.test(input)
    assertResult(expected)(actual)
  }
  /*

  // Configuration
  val config: Config = inject[Config]

  describe("IsFolderEmpty_EmptyFolder") {
    test(S3Manager.isFolderEmpty(config.bucketName, "test/empty/"), true)
  }

  describe("IsFolderEmpty_NotEmptyFolder") {
    test(S3Manager.isFolderEmpty(config.bucketName, "test/notEmpty/"), false)
  }

  describe("IsFolderAtLimit_True") {
    // ensure testing folder is full according to config


    test(S3Manager.isFolderAtLimit(config.bucketName, "test/orderedFilesHourlyExample"), true)
  }

  describe("IsFolderAtLimit_False") {
    test(S3Manager.isFolderAtLimit(config.bucketName, "test/notEmpty"), false)
  }

  describe("ParseStringToDate_ValidDate") {
    // Create a Java Calendar with the time we expect to have
    val expectedCal = Calendar.getInstance()
    expectedCal.set(2017, 7, 9, 17, 33, 44) // Wed Aug 09 17:33:44 EDT 2017
    expectedCal.setTimeZone(TimeZone.getTimeZone("US/Eastern"))
    // Create the Date object from the Cal
    val expectedTime = expectedCal.getTime

    // Parse the String containing the same information that was created above
    val parsedTime: Date =
      S3Manager.parseStringToDate("Wed Aug 09 17:33:44 EDT 2017")

    // Are the string values equal?
    var isEqual: Boolean = false
    if (expectedTime.toString.equals(parsedTime.toString)) {
      isEqual = true
    } else {
      isEqual = false
    }

    test(isEqual, true)
  }

  describe("UploadToFullFolder") {
    val keyCount = S3Manager.getTrueKeyCount(config.bucketName, "test/fullDirectory_Hourly")

    S3Manager.upload(
      config.bucketName,
      "test/fullDirectory_Hourly/",
      s"test_file${scala.util.Random.nextInt(1000)}",
      TestingTools.createSampleFile
    )

    var keyCountsEqual: Boolean = false
    if (S3Manager.getTrueKeyCount(config.bucketName, "test/fullDirectory_Hourly") == keyCount) {
      keyCountsEqual = true
    } else
      keyCountsEqual = false

    test(keyCountsEqual, true)
  }

  describe("UploadToNonFullFolder") {
    val keyCount = S3Manager.getTrueKeyCount(config.bucketName, "test/notFullFolder")

    S3Manager.upload(
      config.bucketName,
      "test/notFullFolder/",
      s"test_file${scala.util.Random.nextInt(1000)}",
      TestingTools.createSampleFile
    )

    var keyCountIncreasedByOne: Boolean = false
    if (S3Manager.getTrueKeyCount(config.bucketName, "test/notFullFolder") == (keyCount + 1)) {
      keyCountIncreasedByOne = true
    } else
      keyCountIncreasedByOne = false

    test(keyCountIncreasedByOne, true)
  }

  describe("PurgeDirectory_NonEmptyDirectory") {

    // Put a file in the directory to test purging
    S3Manager.upload(
      config.bucketName,
      "test/testPurge/",
      s"test_file${scala.util.Random.nextInt(1000)}",
      TestingTools.createSampleFile
    )

    val keyCountWithFiles = S3Manager.getTrueKeyCount(config.bucketName, "test/testPurge/")

    // Purge directory
    S3Manager.purgeDirectory(config.bucketName, "test/testPurge/")

    val keyCountAfterPurge = S3Manager.getTrueKeyCount(config.bucketName, "test/testPurge/")

    var didPurge = false
    if (keyCountWithFiles > 0 && keyCountAfterPurge == 0) {
      didPurge = true
    } else {
      didPurge = false
    }

    test(didPurge, true)

  }

  describe("PurgeDirectory_EmptyDirectory") {

    val keyCountWithFiles = S3Manager.getTrueKeyCount(config.bucketName, "test/empty/")

    // Purge directory
    S3Manager.purgeDirectory(config.bucketName, "test/empty/")

    val keyCountAfterPurge = S3Manager.getTrueKeyCount(config.bucketName, "test/empty/")

    var didPurge = false
    if (keyCountWithFiles == keyCountAfterPurge) {
      didPurge = true
    } else {
      didPurge = false
    }

    test(didPurge, true)
  }

  describe("TestGetStorageClassKeyCount_STANDARD") {

    val standardStorageKeyCount =
      S3Manager.getStandardStorageClassKeyCount(config.bucketName, "test/standardStorageClassObjects/")

    test(standardStorageKeyCount == 2, true)
  }

  */

}
