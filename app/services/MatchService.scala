package services

import java.time.LocalDateTime

import com.google.inject.Inject
import connectors.MongoConnector
import models.{DatabaseFailureResponse, DatabaseResponse, DatabaseSuccessResponse, MatchModel}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MatchService @Inject()(mongoConnector: MongoConnector) {

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

  def getMatch(time: LocalDateTime): Future[DatabaseResponse] = {
    mongoConnector.getData[MatchModel](collectionName, Json.obj("time" -> Json.toJson(time))).map {
      case Some(data) => DatabaseSuccessResponse(Some(data))
      case _ => DatabaseFailureResponse("No matching data found")
    }
  }

  def getMatchesForSeason(season: Int): Future[Seq[MatchModel]] = {
    mongoConnector.getAllData[MatchModel](collectionName, Json.obj("season" -> season))
  }

  def getMatchesForStage(season: Int, stage: Int) : Future[Seq[MatchModel]] = {
    mongoConnector.getAllData[MatchModel](collectionName, Json.obj("season" -> season, "Stage" -> stage))
  }

  def getAllMatches: Future[Seq[MatchModel]] = {
    mongoConnector.getAllData[MatchModel](collectionName, Json.obj())
  }

  def getAllMatchesForTeam(team: String): Future[Seq[MatchModel]] = {
    mongoConnector.getAllData[MatchModel](collectionName, Json.obj("$or" -> Json.arr(
      Json.obj("firstTeam" -> team),
      Json.obj("secondTeam" -> team)
    )))
  }
}
