package ag.s3

// Signify possible frequencies for backups

object BackupFrequency extends Enumeration {
  type BackupFrequency = Value

  val hourly = Value
  val daily = Value
  val monthly = Value

}

