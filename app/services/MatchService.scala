package services

import java.time.LocalDateTime

import com.google.inject.Inject
import connectors.{ApiConnector, MongoConnector}
import models._
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MatchService @Inject()(mongoConnector: MongoConnector,
                             apiConnector: ApiConnector) {

  val collectionName: String = "matches"

  def saveMatch(matchModel: MatchModel): Future[DatabaseResponse] = {
    mongoConnector.saveData(collectionName, matchModel, Json.obj("time" -> Json.toJson(matchModel.time))).map {
      writes =>
        if (writes.ok) DatabaseSuccessResponse()
        else DatabaseFailureResponse(writes.errmsg.getOrElse("Undefined mongo error."))
    }
  }

  def deleteMatch(time: LocalDateTime): Future[DatabaseResponse] = {
    mongoConnector.deleteData(collectionName, Json.obj("time" -> Json.toJson(time))).map {
      writes =>
        if (writes.ok) DatabaseSuccessResponse()
        else DatabaseFailureResponse(writes.writeErrors.headOption.map(_.errmsg).getOrElse("Undefined mongo error"))
    }
  }

  def getMatch(id: String): Future[Option[NewMatchModel]] = {
    apiConnector.getMatch(id)
  }

  def getMatchesForSeason(season: String): Future[SeasonModel] = {
    apiConnector.getSeason(season)
  }

  def getMatchesForStage(season: String, stage: Int) : Future[Option[StageModel]] = {
    getMatchesForSeason(season).map {
      _.stages.find(_.name == s"Stage $stage")
    }
  }

  def getAllMatches: Future[Seq[SeasonModel]] = {
    //TODO add configuration variables
    val seasons: Seq[String] = Seq("2018", "2019")
    Future.sequence(seasons.map(getMatchesForSeason))
  }

  def getAllMatchesForTeam(team: String): Future[Seq[SeasonModel]] = {
    getAllMatches.map {
      _.map(_.filterByTeam(team))
    }
  }
}
