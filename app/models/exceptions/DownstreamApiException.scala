package models.exceptions

case class ServiceException(message: String) extends Exception(message)

case class DownstreamApiException(message: String, status: Int) extends Exception(message)
