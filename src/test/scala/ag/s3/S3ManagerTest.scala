package ag.s3

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

  describe("ParseStringToDate_20170818_102003") {

  }

}
