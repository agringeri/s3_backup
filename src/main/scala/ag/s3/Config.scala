package ag.s3

import com.google.inject.{Inject, Singleton}

@Singleton
case class Config @Inject()(
  gitlab: Gitlab,
  postgres: Postgres,
  website: Website
)

trait BackupTypes {
  def hourly: Int
  def daily: Int
  def monthly: Int
}

case class Gitlab (
  hourly: Int,
  daily: Int,
  monthly: Int
) extends BackupTypes

case class Postgres (
  hourly: Int,
  daily: Int,
  monthly: Int
) extends BackupTypes

case class Website(
  hourly: Int,
  daily: Int,
  monthly: Int
) extends BackupTypes