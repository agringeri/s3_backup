package ag.s3

import scala.collection.JavaConverters._
import java.io._
import java.util
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAmount

import com.amazonaws.auth._
import com.amazonaws._
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import m3.guice.DaemonApp
import m3.predef.inject

object S3Manager extends DaemonApp {

  val config: Config = inject[Config]
  val awsConfig: AWSConfig = inject[AWSConfig]

  // Set up AWS credentials
  val credentials: BasicAWSCredentials = new BasicAWSCredentials(
    awsConfig.accessKey,
    awsConfig.secretKey,
  )

  // Establish S3 Client
  val s3: AmazonS3 =
    AmazonS3ClientBuilder
      .standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withRegion(awsConfig.region)
      .withForceGlobalBucketAccessEnabled(true)
      .build()

  /**
    * Uploads a backup file where necessary, when necessary.
    * Acts as entry point to rest of methods
    * @param backupCategory
    * @param fileName
    * @param file
    */
  def uploadBackup(backupCategory: BackupCategory.Value, fileName: String, file: File): Unit = {

    // Check if files under standard storage need to be purged (if config limit was lowered)
    val frequencies = BackupFrequency.values
    val categories = BackupCategory.values
    // Traverse all folders specified in BackupFrequency and BackupCategory
    for (frequency <- frequencies) {
      for (category <- categories) {

        // This directory's summaries
        // Ignore files that may have been moved to GLACIER (don't want to mess with those, leave that up to bucket lifecycle rules)
        val thisDirectorySummaries = getStandardStorageClassSummaries(getTrueObjectSummaries(frequency, category))

        // Delete old keys in folder if needed
        val numExpiredKeys =
          getNumKeysAboveLimit(thisDirectorySummaries, getFolderLimit(category, frequency))

        if (numExpiredKeys > 0) {
          for (i <- 1 to numExpiredKeys) {
            val oldestKey = getOldestKey(thisDirectorySummaries)
            oldestKey.foreach { key =>
              s3.deleteObject(awsConfig.bucketName, key)
              logger.info(s"Object deleted (${key})")
            }
          }
        }

        // Is a new backup needed here?
        val isBackupNeeded = doesFolderNeedBackup(category, frequency, getStandardStorageClassSummaries(getTrueObjectSummaries(frequency, category)))

        // If the category matches and a backup is needed
        // Currently set to always backup to hourly, regardless if one has been placed within the hour
        if (backupCategory.equals(category) && isBackupNeeded) {

          // Upload new file
          putFile(frequency, category, fileName, file)
          logger.info(s"New ${category} backup uploaded: (${getPath(frequency, category)}${fileName})")

          // If the limit is now exceeded, delete the oldest one
          if (isFolderAboveLimit(category, frequency)) {
            val oldestKey = getOldestKey(getStandardStorageClassSummaries(getTrueObjectSummaries(frequency, category))).getOrElse("")
            deleteFile(oldestKey)
            logger.info(s"Old ${category} backup deleted: ${oldestKey}")
          }
        }
      }
    }
  }

  /**
    * Puts a file in S3
    * @param backupFrequency frequency of backup
    * @param backupCategory category of backup
    * @param fileName the name of the file (just file name, not full path)
    * @param file the file to put
    */
  def putFile(backupFrequency: BackupFrequency.Value, backupCategory: BackupCategory.Value, fileName: String, file: File): Unit = {
    // Create request to put object
    val putObjectRequest = new PutObjectRequest(
      awsConfig.bucketName,
      s"${getPath(backupFrequency, backupCategory)}$fileName",
      file
    ).withMetadata(generateTimeStampMetadata())

    try {
      // Attempt to upload the object with the proper metadata
      s3.putObject(putObjectRequest)
    } catch {
      case ase: AmazonServiceException =>
        val msg =
          s"""Caught an AmazonServiceException, which means your request made it
            to Amazon S3, but was rejected with an error response for some reason.

            ${ase.getMessage}
            ${ase.getStatusCode}
            ${ase.getErrorCode}
            ${ase.getErrorType}
            ${ase.getRequestId}
        """
        logger.warn(msg, ase)

      case ace: AmazonClientException =>
        val msg =
          s"""Caught an AmazonClientException, which means the client encountered
            a serious internal problem while trying to communicate with S3,
            such as not being able to access the network.

            ${ace.getMessage}
        """
        logger.warn(msg, ace)
    }
  }

  /**
    * Deletes a file from S3
    * @param key the key of the file to delete
    */
  def deleteFile(key: String): Unit = {
    val deleteObjectRequest = new DeleteObjectRequest(
      awsConfig.bucketName,
      key
    )

    try {
      // Attempt to delete object
      s3.deleteObject(deleteObjectRequest)
    } catch {
      case ase: AmazonServiceException =>
        val msg =
          s"""Caught an AmazonServiceException, which means your request made it
            to Amazon S3, but was rejected with an error response for some reason.

            ${ase.getMessage}
            ${ase.getStatusCode}
            ${ase.getErrorCode}
            ${ase.getErrorType}
            ${ase.getRequestId}
        """
        logger.warn(msg, ase)

      case ace: AmazonClientException =>
        val msg =
          s"""Caught an AmazonClientException, which means the client encountered
            a serious internal problem while trying to communicate with S3,
            such as not being able to access the network.

            ${ace.getMessage}
        """
        logger.warn(msg, ace)
    }
  }

  /**
    * Purges (completely empties) a given directory (prefix) in an Amazon S3 bucket
    *
    * @param backupFrequency frequency of backup to purge (ex: hourly, daily, monthly)
    * @param backupCategory type of backup to purge (ex: gitlab, postgres)
    */
  def purgeEntireDirectory(backupFrequency: BackupFrequency.Value, backupCategory: BackupCategory.Value): Unit = {

    val request = new ListObjectsV2Request()
      .withBucketName(awsConfig.bucketName)
      .withPrefix(
        s"${backupFrequency.toString.toLowerCase}/${backupCategory.toString.toLowerCase}/"
      )

    val summaries = getOnlyTrueObjects(
      s3.listObjectsV2(request).getObjectSummaries
    )

    for (i <- 0 until summaries.size) {
      val keyToDelete = summaries.get(i).getKey
      s3.deleteObject(awsConfig.bucketName, keyToDelete)
      logger.debug(s"Object deleted:(${keyToDelete})")
    }

  }

  /**
    * Returns whether a folder needs a backup or not
    * @param backupCategory category of backup
    * @param backupFrequency frequency of backup
    * @param summaries the summaries from the folder to check
    * @return true if folder needs backup, false if not
    */
  def doesFolderNeedBackup(backupCategory: BackupCategory.Value, backupFrequency: BackupFrequency.Value, summaries: util.List[S3ObjectSummary]): Boolean = {

    val dateTimeNow = LocalDateTime.now

    val newestKey = getNewestKey(summaries).getOrElse(return true)

    val localDateTime: Option[LocalDateTime] =
      parseStringToDate(
        s3
          .getObjectMetadata(awsConfig.bucketName, newestKey)
          .getUserMetaDataOf("date-created")
      )

    val localDateTimeNow: LocalDateTime =
      dateTimeNow
        .minus(getDuration(backupFrequency))

    val isAfter: Boolean =
      localDateTime
        .exists(ldt => ldt.isAfter(localDateTimeNow))

    val isNow: Boolean =
      localDateTime.exists(ldt => ldt.isEqual(localDateTimeNow))

    if ( isAfter || isNow) {
      false
    } else {
      true
    }

  }

  /**
    * Gets the number of files that are above the limit, if any
    * @param summaries the List of S3 summaries
    * @param limit the limit to check
    * @return the number of files that are above the limit, if any
    */
  def getNumKeysAboveLimit(summaries: util.List[S3ObjectSummary], limit: Int): Int = {
    if (summaries.size > limit) {
      summaries.size - limit
    } else {
      0
    }
  }

  /**
    * Returns the newest key (based on metadata) from a listing
    * @param summaries the List of S3 summaries
    * @return the newest key, if there is one
    */
  def getNewestKey(summaries: util.List[S3ObjectSummary]): Option[String] = {
    if (isSummaryListingEmpty(summaries)) {
      None

    } else {

      var currentNewestItem = (
        summaries.get(0).getKey,
        s3.getObjectMetadata(awsConfig.bucketName, summaries.get(0).getKey).getUserMetaDataOf("date-created")
      )

      for (summary <- summaries.asScala) {
        val thisDate =
          parseStringToDate(s3.getObjectMetadata(awsConfig.bucketName, summary.getKey).getUserMetaDataOf("date-created")).get
        if (thisDate.isAfter(parseStringToDate(currentNewestItem._2).get))
        {
          currentNewestItem = (
            summary.getKey,
            s3.getObjectMetadata(awsConfig.bucketName, summary.getKey).getUserMetaDataOf("date-created")
          )
        }
      }

      Some(currentNewestItem._1)
    }
  }

  /**
    * Returns the oldest key (based on metadata) from a listing
    * @param summaries the List of S3 summaries
    * @return the oldest key, if there is one
    */
  def getOldestKey(summaries: util.List[S3ObjectSummary]): Option[String] = {
    if (isSummaryListingEmpty(summaries)) {
      None

    } else {

      var currentOldestItem = (
        summaries.get(0).getKey,
        s3.getObjectMetadata(awsConfig.bucketName, summaries.get(0).getKey).getUserMetaDataOf("date-created")
      )

      for (summary <- summaries.asScala) {
        val thisDate =
          parseStringToDate(s3.getObjectMetadata(awsConfig.bucketName, summary.getKey).getUserMetaDataOf("date-created")).get
        if (thisDate.isBefore(parseStringToDate(currentOldestItem._2).get))
        {
          currentOldestItem = (
            summary.getKey,
            s3.getObjectMetadata(awsConfig.bucketName, summary.getKey).getUserMetaDataOf("date-created")
          )
        }
      }

      Some(currentOldestItem._1)
    }
  }

  /**
    * Checks if a given folderName is empty (has any files)
    * @return true if given folder (path) is empty (contains no objects), false otherwise
    */
  def isSummaryListingEmpty(summaries: util.List[S3ObjectSummary]): Boolean = {

    val trueObjects = getOnlyTrueObjects(summaries)

    if (trueObjects.size <= 0) {
      true
    } else {
      false
    }
  }

  /**
    * Checks if the folder is at OR ABOVE the specified limit according to config
    * @param backupCategory category of backup
    * @param backupFrequency frequency of backup
    * @return true if folder is strictly > corresponding limit, false otherwise
    */
  def isFolderAboveLimit(backupCategory: BackupCategory.Value, backupFrequency: BackupFrequency.Value): Boolean = {
    val request = new ListObjectsV2Request()
      .withBucketName(awsConfig.bucketName)
      .withPrefix(s"${backupFrequency.toString.toLowerCase}/${backupCategory.toString.toLowerCase}/")

    val objectSummaries =
      getStandardStorageClassSummaries(
        getOnlyTrueObjects(
          s3.listObjectsV2(request).getObjectSummaries
        )
      )

    val keyCount = objectSummaries.size

    if (keyCount > getFolderLimit(backupCategory, backupFrequency)) {
      true
    } else {
      false
    }
  }


  /**
    * Returns the limit for a given folder
    * @param backupCategory category of backup
    * @param backupFrequency frequency of backup
    * @return the limit of a given folder
    */
  def getFolderLimit(backupCategory: BackupCategory.Value, backupFrequency: BackupFrequency.Value): Int = {
    val backupType: BackupTypes =
      backupCategory match {
        case BackupCategory.gitlab =>
          config.gitlab
        case BackupCategory.postgres =>
          config.postgres
        case BackupCategory.website =>
          config.website
        case unsupported =>
          throw new RuntimeException(s"getFolderLimit -- does not support $unsupported")
      }

    backupFrequency match {
      case BackupFrequency.hourly =>
        backupType.hourly
      case BackupFrequency.daily =>
        backupType.daily
      case BackupFrequency.monthly =>
        backupType.monthly
    }
  }

  /**
    * Parses a String to a Date object
    * Used for analyzing metadata (which is in String form)
    *
    * Format: yyyyMMdd'_'HHmmss
    *
    * Sample: "20170817_162042" -> Corresponding DateTime Object
    *
    * @param s the string being parsed
    * @return the DateTime object corresponding to format
    */
  def parseStringToDate(s: String): Option[LocalDateTime] = {

    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmmss")

    try {
      val dateTime = LocalDateTime.from(formatter.parse(s))
      Some(dateTime)
    } catch {
      case iae : IllegalArgumentException =>
        logger.warn("Method parseStringToDate received a String in an incorrect format. Unable to generate Date object", iae)
        None
    }
  }

  /**
    * Generates a S3 ObjectMetaData object containing current timestamp
    *
    * @return ObjectMetadata object with current-time metadata
    */
  def generateTimeStampMetadata(): ObjectMetadata = {
    val f = DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmmss")
    val thisTimeStamp = LocalDateTime.now.format(f)

    val metaDataToAdd: ObjectMetadata = new ObjectMetadata()
    metaDataToAdd.addUserMetadata("date-created", thisTimeStamp)
    metaDataToAdd
  }

  /**
    * Get a List of S3ObjectSummaries that are in the STANDARD storage class.
    *
    * @param summaries List of S3ObjectSummaries
    * @return List of S3ObjectSummaries that match the prefix and are in the STANDARD storage class
    */
  def getStandardStorageClassSummaries(summaries: util.List[S3ObjectSummary]): util.List[S3ObjectSummary] = {
    val newSummaries = summaries

    val iter = newSummaries.iterator
    while (iter.hasNext) {
      val item = iter.next

      if (!item.getStorageClass.equals("STANDARD")) {
        iter.remove()
      }
    }

    newSummaries
  }

  /**
    * Get a List of S3ObjectSummaries that are in the GLACIER storage class.
    *
    * @param summaries the original List of S3ObjectSummaries
    * @return List of S3ObjectSummaries that are in the GLACIER storage class
    */
  def getGlacierStorageClassSummaries(summaries: util.List[S3ObjectSummary]): util.List[S3ObjectSummary] = {
    val newSummaries = summaries

    val iter = summaries.iterator
    while (iter.hasNext) {
      val item = iter.next

      if (!item.getStorageClass.equals("GLACIER")) {
        iter.remove()
      }
    }

    newSummaries
  }

  /**
    * Get a List of S3ObjectSummaries that are in the STANDARD_IA storage class.
    *
    * @param summaries the original List of S3ObjectSummaries
    * @return List of S3ObjectSummaries that are in the STANDARD_IA storage class
    */
  def getStandardIAStorageClassSummaries(summaries: util.List[S3ObjectSummary]): util.List[S3ObjectSummary] = {
    val newSummaries = summaries

    val iter = summaries.iterator
    while (iter.hasNext) {
      val item = iter.next

      if (!item.getStorageClass.equals("STANDARD_IA")) {
        iter.remove()
      }
    }

    newSummaries
  }

  /**
    * Returns only the summaries in the scope of the frequency and category
    * @param backupFrequency frequency of backup
    * @param backupCategory category of backup
    * @return the summaries within scope
    */
  def getTrueObjectSummaries(backupFrequency: BackupFrequency.Value, backupCategory: BackupCategory.Value): util.List[S3ObjectSummary] = {
    val pathToObjects = s"${backupFrequency}/${backupCategory}/"

    getOnlyTrueObjects(
      s3.listObjectsV2(
        awsConfig.bucketName,
        pathToObjects
      ).getObjectSummaries
    )
  }

  /**
    * Filters an S3ObjectSummary List by removing summaries that refer to directories, not objects
    *
    * For example, if listObjects were performed on a bucket with prefix "hourly/",
    * an example of the listing of keys would be:
    *
    * "hourly/"
    * "hourly/backup1"
    * "hourly/backup2"
    * "hourly/backup3"
    *
    * This method removes the "hourly/" listing
    *
    * @param summaries the List of S3ObjectSummary
    * @return new list of S3ObjectSummary without directory listings
    */
  def getOnlyTrueObjects(summaries: util.List[S3ObjectSummary]): util.List[S3ObjectSummary] = {

    val trueSummaries = summaries

    // Iterate through summaries and remove any summaries refer to "directories"
    val iter = trueSummaries.iterator
    while (iter.hasNext) {
      val item = iter.next

      if (item.getKey.endsWith("/")) {
        iter.remove()
      }
    }

    trueSummaries
  }

  /**
    * Get the path to a file using the frequency and category of a backup
    * @param backupFrequency frequency of backup
    * @param backupCategory category of backup
    * @return the path to the file
    */
  def getPath(backupFrequency: BackupFrequency.Value, backupCategory: BackupCategory.Value): String = {
    s"${backupFrequency}/${backupCategory}/"
  }

  /**
    * Matches a string frequency (hourly, daily, etc) to a TemporalAmount representing a length of time
    * @param backupFrequency frequency of backup
    * @return the length of time represented by backupFrequency
    */
  def getDuration(backupFrequency: BackupFrequency.Value): TemporalAmount = {
    backupFrequency match {
      case BackupFrequency.hourly => Duration.ofHours(1)
      case BackupFrequency.daily => Period.ofDays(1)
      case BackupFrequency.monthly => Period.ofMonths(1)
    }
  }

  def test(b: Boolean) = b.equals(true)

}