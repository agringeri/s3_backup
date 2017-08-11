package ag.s3

import scala.collection.JavaConverters._
import java.io._
import java.text._
import java.util
import java.util._

import com.amazonaws.auth._
import com.amazonaws._
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3ObjectSummary
import m3.guice.DaemonApp
import m3.predef.inject

object S3Manager extends DaemonApp {

  // Inject Configuration
  val config: Config = inject[Config]

  // Set up AWS credentials
  val credentials: BasicAWSCredentials = new BasicAWSCredentials(
    config.accessKey,
    config.secretKey,
  )

  // Establish S3 Client
  val s3: AmazonS3 =
    AmazonS3ClientBuilder
      .standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withRegion("us-east-1")
      .withForceGlobalBucketAccessEnabled(true)
      .build()

  /**
    * Uploads a file to Amazon S3
    *
    * If there are LESS files present in the given location than specified in the Config
    * - The new file will be uploaded and no files will be removed
    *
    * If there are MORE files present in the given location than specified in the Config
    * - The new file will be uploaded and the oldest existing file in the location will be removed
    *
    * @param bucketName the name of the Amazon S3 bucket
    * @param location the location of the object to be uploaded (omit file name, path should end in "/")
    * @param fileName the filename of the object to be uploaded
    * @param file object to be uploaded
    */
  def upload(bucketName: String, location: String, fileName: String, file: File): Unit = {
    val fullPath: String =
      location + fileName
    val isFolderAtLimitValue: Boolean =
      isFolderAtLimit(bucketName, location)

    try {

      if (!isFolderAtLimitValue) {

        // Just upload
        s3.putObject(new PutObjectRequest(bucketName, fullPath, file).withMetadata(generateTimeStampMetadata()))
        val objectMetadata = s3.getObjectMetadata(bucketName, fullPath).getUserMetaDataOf("date-created")
        logger.debug(s"Object (${fullPath}) added to bucket (${bucketName})")
        logger.debug(s"Object (${fullPath}) has the following creation-date: (${objectMetadata})")

      } else {

        // Upload and delete oldest object
        s3.putObject(new PutObjectRequest(bucketName, fullPath, file).withMetadata(generateTimeStampMetadata()))
        val deletedObjectKey = deleteOldestObject(bucketName, location)
        val objectMetadata = s3.getObjectMetadata(bucketName, fullPath).getUserMetaDataOf("date-created")
        logger.debug(s"Object (${deletedObjectKey.orNull}) deleted from bucket ($bucketName), object (${fullPath}) added to bucket (${bucketName})")
        logger.debug(s"Object (${fullPath}) has the following creation-date: (${objectMetadata})")
      }

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
    * Deletes file (given its key) from Amazon S3
    *
    * @param bucketName the name of the Amazon S3 bucket
    * @param key the filename of the object to be deleted
    */
  def delete(bucketName: String, key: String): Unit = {
    try {

      if (s3.doesObjectExist(bucketName, key)) {
        s3.deleteObject(bucketName, key)
        logger.debug(s"File (${key}) deleted from bucket (${bucketName})")

      } else {

        logger.warn(s"Delete request failed: File (${key}) does not exist in bucket (${bucketName})")

      }

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
    * Delete oldest object within given location in a bucket
    *
    * For example, calling:
    *   deleteOldestObject("bucket-name", "daily")
    * will delete the oldest object within the "bucket-name" bucket, and within the "daily" folder
    *
    * @param bucketName the name of the Amazon S3 bucket
    * @param location the location of the directory to be checked
    * @return the key of the deleted object
    */
  def deleteOldestObject(bucketName: String, location: String): Option[String] = {

    var oldestObjectKey: Option[String] = None

    // Check that there are files in the folder
    if (!isFolderEmpty(bucketName, location)) {

      // Retrieve first object for later comparison
      oldestObjectKey = Some(filterSummaries(s3.listObjectsV2(bucketName, location).getObjectSummaries, location).get(0).getKey)

      // Iterate over all objectListings and find earliest creation date
      val listObjectsRequest = new ListObjectsV2Request()
        .withBucketName(bucketName)
        .withPrefix(location)

      var objectListing = new ListObjectsV2Result()
      objectListing = s3.listObjectsV2(listObjectsRequest)
      val filteredObjectSummaries = filterSummaries(objectListing.getObjectSummaries, location)

      for (objectSummary <- filteredObjectSummaries.asScala) {

        // Retrieve metadata of current object
        val currentCreationDate = s3.getObjectMetadata(bucketName, objectSummary.getKey).getUserMetaDataOf("date-created")
        logger.warn(s3.getObjectMetadata(bucketName, objectSummary.getKey).getUserMetaDataOf("date-created"))

        // Is current object's date-created before the "oldest"?
        if (parseStringToDate(currentCreationDate)
          .before(parseStringToDate(s3.getObjectMetadata(bucketName, oldestObjectKey.get) // getOrElse should never occur
            .getUserMetaDataOf("date-created"))))
        { oldestObjectKey = Some(objectSummary.getKey) }

      }

      // Oldest object is identified, now delete it
      S3Manager.delete(bucketName, oldestObjectKey.get)

    } else {

      // Log error warning and return None (nothing deleted)
      logger.warn(s"There are no objects contained in bucket (${bucketName}) in folder (${location})")

    }

    // Returns None if nothing was deleted (i.e. empty directory)
    oldestObjectKey
  }

  def getObjects(bucketName: String, prefix: String): ListObjectsV2Result = {
    s3.listObjectsV2(bucketName, prefix)
  }

  /**
    * Checks if a given folderName is empty (has any files)
    *
    * @param bucketName the name of the Amazon S3 bucket
    * @param folderName the name of the folder (or path) to check
    * @return true if given folder (or path) is empty, false otherwise
    */
  def isFolderEmpty(bucketName: String, folderName: String): Boolean = {
    val request = new ListObjectsV2Request()
      .withBucketName(bucketName)
      .withPrefix(folderName)
      .withDelimiter("/")

    val filteredSummaries: util.List[S3ObjectSummary] =
      filterSummaries(
        s3.listObjectsV2(request).getObjectSummaries,
        folderName
      )

    for (summary <- filteredSummaries.asScala) {
      logger.warn(summary.getKey)
    }

    if (filteredSummaries.size <= 0) {
      true
    } else {
      false
    }
  }

  /**
    * Checks if the folder is at the specified limit according to Config
    *
    * For example, calling
    *   isFolderAtLimit("bucket-name", "daily")
    * will return true if the maximum number of daily backups is reached, and false otherwise
    *
    * @param bucketName the name of the Amazon S3 bucket
    * @param folderName name of folder (or path) to check
    * @return true if folder is >= corresponding limit, false otherwise
    */
  def isFolderAtLimit(bucketName: String, folderName: String): Boolean = {
    val objectSummaries =
      s3.listObjectsV2(bucketName, folderName).getObjectSummaries

    filterSummaries(objectSummaries, folderName)

    if (objectSummaries.size >= getFolderLimit(folderName)) {
      true
    } else {
      false
    }
  }

  /**
    * Returns the limit for a given folder
    *
    * Note: this method will discard the prefix of the folderName and only match against the last argument
    * Example: passing ("test/myFolder/hourly") will match successfully against "hourly"
    *
    * @param folderName the name of folder (or path)
    * @return the limit of the folder according to config
    */
  def getFolderLimit(folderName: String): Int = {
    // Just give last part of folderName, discard prefix
    (folderName.split("/").min) match {
      case "hourly" => config.s3BackupLimits.hourly
      case "daily" => config.s3BackupLimits.daily
      case "monthly" => config.s3BackupLimits.monthly

      // TODO: Remove this before deployment, this is for testing
      case "orderedFilesHourlyExample" => config.s3BackupLimits.hourly
      case "notEmpty" => config.s3BackupLimits.hourly
      case "fullDirectory_Hourly" => config.s3BackupLimits.hourly

      case _ => -1
    }
  }

  /**
    * Parses a String to a Date object
    * Used for analyzing metadata (which is in String form)
    *
    * Sample: "Wed Aug 09 17:33:44 EDT 2017" -> Corresponding Date Object
    *
    * @param s the string being parsed
    * @return the date object corresponding to format
    */
  def parseStringToDate(s: String): Date = {

    val cal = Calendar.getInstance
    val sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")

    try {
      cal.setTime(sdf.parse(s))
    } catch {
      case pe : ParseException =>
        logger.warn("Method parseStringToDate received a String in an incorrect format. Unable to generate Date object", pe)
    }

    cal.getTime
  }

  /**
    * Generates a S3 ObjectMetaData object containing current timestamp
    *
    * @return ObjectMetadata object with current-time metadata
    */
  def generateTimeStampMetadata(): ObjectMetadata = {
    val thisDate: String = Calendar.getInstance(TimeZone.getTimeZone("US/Eastern")).getTime.toString
    val metaDataToAdd: ObjectMetadata = new ObjectMetadata()
    metaDataToAdd.addUserMetadata("date-created", thisDate)
    metaDataToAdd
  }

  /**
    * Filters an S3ObjectSummary List by removing itself in it's own object listing
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
    * @param summaries the list of S3ObjectSummary
    * @param filter the name of the folder to filter out
    * @return new list of S3ObjectSummary without itself in it
    */
  def filterSummaries(summaries: util.List[S3ObjectSummary], filter: String): util.List[S3ObjectSummary] = {

    val filteredSummaries: util.List[S3ObjectSummary] = summaries

    // Iterate through summaries and remove any summaries that are self-descriptive (equal to folderName argument)
    val iter = filteredSummaries.iterator
    while (iter.hasNext) {
      val item = iter.next

      if (item.getKey.equals(filter)) {
        iter.remove()
      }
    }

    filteredSummaries
  }

  def test(b: Boolean) = b.equals(true)
}
