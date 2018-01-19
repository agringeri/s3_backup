package ag.s3

// Signify possible backup categories

object BackupCategory extends Enumeration {
  type BackupCategory = Value

  val gitlab = Value
  val postgres = Value
  val website = Value

}