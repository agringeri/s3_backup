package ag.s3

import java.time._
import java.time.format.DateTimeFormatter
import java.util
import java.util._

import com.amazonaws.services.s3.model.S3ObjectSummary
import m3.predef._
import org.scalatest.FunSpec

class S3ManagerTest extends FunSpec with Logging {

  import m3.test.FunSpecMacros._

  implicit def runner(input:Boolean, expected: Boolean): Unit = {
    val actual = S3Manager.test(input)
    assertResult(expected)(actual)
  }

  describe("GetDuration_Valid") {
    val duration = S3Manager.getDuration(BackupFrequency.daily)
    test(duration.toString.equals(Period.ofDays(1).toString), true)
  }

  describe("GetPath") {
    val path = S3Manager.getPath(BackupFrequency.hourly, BackupCategory.gitlab)
    test(path.equals("hourly/gitlab/"), true)
  }

  describe("IsSummaryListingEmpty_True") {
    val list = new util.ArrayList[S3ObjectSummary]()

    test(S3Manager.isSummaryListingEmpty(list), true)
  }

  describe("IsSummaryListingEmpty_False") {
    val summary = new S3ObjectSummary()
    summary.setKey("hourly/gitlab/gitlab_2017010100_120000")

    val list = new util.ArrayList[S3ObjectSummary]()
    list.add(summary)

    test(S3Manager.isSummaryListingEmpty(list), false)
  }

  describe("GetStandardStorageClassSummaries_One") {
    val summaryStandard = new S3ObjectSummary()
    summaryStandard.setKey("hourly/gitlab/gitlab_2017010100_120000")
    summaryStandard.setStorageClass("STANDARD")

    val summaryGlacier = new S3ObjectSummary()
    summaryGlacier.setKey("hourly/gitlab/gitlab_2016010100_120000")
    summaryGlacier.setStorageClass("GLACIER")

    val list = new util.ArrayList[S3ObjectSummary]()
    list.add(summaryStandard)
    list.add(summaryGlacier)

    test(
      S3Manager.getStandardStorageClassSummaries(list).get(0).getStorageClass.equals("STANDARD") &&
      S3Manager.getStandardStorageClassSummaries(list).size.equals(1),
      true)
  }

  describe("GetNumKeysAboveLimit_2") {
    val summaryStandard = new S3ObjectSummary()
    summaryStandard.setKey("hourly/gitlab/gitlab_2017010100_120000")
    summaryStandard.setStorageClass("STANDARD")

    val summaryGlacier = new S3ObjectSummary()
    summaryGlacier.setKey("hourly/gitlab/gitlab_2016010100_120000")
    summaryGlacier.setStorageClass("GLACIER")

    val list = new util.ArrayList[S3ObjectSummary]()
    list.add(summaryStandard)
    list.add(summaryGlacier)

    test(S3Manager.getNumKeysAboveLimit(list, 0).equals(2), true)
  }

  describe("GetNumKeysAboveLimit_0") {
    val summaryStandard = new S3ObjectSummary()
    summaryStandard.setKey("hourly/gitlab/gitlab_2017010100_120000")
    summaryStandard.setStorageClass("STANDARD")

    val summaryGlacier = new S3ObjectSummary()
    summaryGlacier.setKey("hourly/gitlab/gitlab_2016010100_120000")
    summaryGlacier.setStorageClass("GLACIER")

    val list = new util.ArrayList[S3ObjectSummary]()
    list.add(summaryStandard)
    list.add(summaryGlacier)

    test(S3Manager.getNumKeysAboveLimit(list, 3).equals(0), true)
  }

}
