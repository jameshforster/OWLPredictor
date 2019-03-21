package services

import com.google.inject.Inject
import connectors.MongoConnector
import models.{DatabaseFailureResponse, DatabaseResponse, DatabaseSuccessResponse, TeamModel}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TeamService @Inject()(mongoConnector: MongoConnector) {

  val collectionName = "teams"

  def saveTeam(team: TeamModel): Future[DatabaseResponse] = {
    mongoConnector.saveData(collectionName, team, Json.obj("name" -> Json.toJson(team.name))).map {
      writes =>
        if (writes.ok) DatabaseSuccessResponse()
        else DatabaseFailureResponse(writes.errmsg.getOrElse("Undefined mongo error."))
    }
  }

  def deleteTeam(name: String): Future[DatabaseResponse] = {
    mongoConnector.deleteData(collectionName, Json.obj("name" -> Json.toJson(name))).map {
      writes =>
        if (writes.ok) DatabaseSuccessResponse()
        else DatabaseFailureResponse(writes.writeErrors.headOption.map(_.errmsg).getOrElse("Undefined mongo error"))
    }
  }

  def getTeam(name: String): Future[DatabaseResponse] = {
    mongoConnector.getData[TeamModel](collectionName, Json.obj("name" -> Json.toJson(name))).map {
      case Some(data) => DatabaseSuccessResponse(Some(data))
      case _ => DatabaseFailureResponse("No matching data found")
    }
  }
}
