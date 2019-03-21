package models

sealed trait DatabaseResponse

case class DatabaseSuccessResponse[A](data: Option[A] = None) extends DatabaseResponse

case class DatabaseFailureResponse(reason: String) extends DatabaseResponse