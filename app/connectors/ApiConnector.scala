package connectors

import com.google.inject.Inject
import models.{CompetitorsModel, NewMatchModel, SeasonModel}
import models.exceptions.{DownstreamApiException, ServiceException}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiConnector @Inject()(wSClient: WSClient) {
  val logger = Logger("[API Connector]")

  def baseUrl(urlComponent: String) = s"https://api.overwatchleague.com/$urlComponent"

  def getTeams: Future[CompetitorsModel] = {
    wSClient.url(baseUrl("teams")).get().map {
      case response if response.status == 200 => response.json.validate[CompetitorsModel] match {
        case JsSuccess(data, _) => data
        case JsError(error) =>
          logger.error(s"${response.body}")
          logger.error(s"Errors parsing json: ${error.map(_._2)}")
          throw ServiceException("Errors parsing json")
      }
      case response =>
        logger.error(s"Unexpected ${response.status} response from API")
        throw DownstreamApiException(response.body, response.status)
    }
  }

  def getSeason(season: String): Future[SeasonModel] = {
    wSClient.url(baseUrl("schedule"))
      .withQueryStringParameters(("season", season))
      .get().map {
      case response if response.status == 200 => response.json.validate[SeasonModel] match {
        case JsSuccess(data, _) if data.id == season => data
        case JsSuccess(_, _) => throw DownstreamApiException(s"Season for id: $season does not exist yet", 404)
        case JsError(errors) =>
          logger.error(s"${response.body}")
          errors.foreach(error =>
            logger.error(s"Errors parsing json for [getSeason] Json path: ${error._1}, errors: ${error._2}")
          )
          throw ServiceException("Errors parsing json")
      }
      case response =>
        logger.error(s"Unexpected ${response.status} response from API")
        throw DownstreamApiException(response.body, response.status)
    }
  }

  def getMatch(id: String): Future[Option[NewMatchModel]] = {
    wSClient.url(baseUrl(s"match/$id")).get().map {
      case response if response.status == 200 => response.json.validate[NewMatchModel] match {
        case JsSuccess(data, _) => Some(data)
        case JsError(errors) =>
          logger.error(s"${response.body}")
          errors.foreach(error =>
            logger.error(s"Errors parsing json for [getMatch] Json path: ${error._1}, errors: ${error._2}")
          )
          throw ServiceException("Errors parsing json")
      }
      case response if response.status == 404 => None
      case response =>
        logger.error(s"Unexpected ${response.status} response from API")
        throw DownstreamApiException(response.body, response.status)
    }
  }
}
