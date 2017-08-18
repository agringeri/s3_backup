package ag.s3

import com.google.inject.{Inject, Singleton}

@Singleton
case class Config @Inject()(
  gitlab: Gitlab,
  postgres: Postgres,
)

case class Gitlab (
  hourly: Int,
  daily: Int,
  monthly: Int,
)

case class Postgres (
  hourly: Int,
  daily: Int,
  monthly: Int,
)