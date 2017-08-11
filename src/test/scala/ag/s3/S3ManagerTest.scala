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

    logger.warn(expectedTime.getTime.toString)
    logger.warn(parsedTime.getTime.toString)

    // Are the string values equal?
    var isEqual: Boolean = false
    if (expectedTime.toString.equals(parsedTime.toString)) {
      isEqual = true
    } else {
      isEqual = false
    }

    test(isEqual, true)
  }


}
