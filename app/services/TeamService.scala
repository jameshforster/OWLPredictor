package services

import com.google.inject.Inject
import connectors.{ApiConnector, MongoConnector}
import models._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamService @Inject()(mongoConnector: MongoConnector, apiConnector: ApiConnector) {

  val collectionName = "teams"

  def saveTeam(team: TeamModel): Future[DatabaseResponse] = {
    mongoConnector.saveData(collectionName, team, Json.obj("name" -> Json.toJson(team.name))).map {
      writes =>
        if (writes.ok) DatabaseSuccessResponse()
        else DatabaseFailureResponse(writes.errmsg.getOrElse("Undefined mongo error."))
    }
  }

  def deleteTeam(identifier: String): Future[DatabaseResponse] = {
    mongoConnector.deleteData(collectionName, Json.obj("identifier" -> Json.toJson(identifier))).map {
      writes =>
        if (writes.ok) DatabaseSuccessResponse()
        else DatabaseFailureResponse(writes.writeErrors.headOption.map(_.errmsg).getOrElse("Undefined mongo error"))
    }
  }

  def getTeam(identifier: String): Future[Option[CompetitorWithDivisionModel]] = {
    apiConnector.getTeams.map(_.competitors.find(_.competitor.abbreviatedName == identifier))
  }

  def getAllTeams: Future[Seq[CompetitorWithDivisionModel]] = {
    apiConnector.getTeams.map(_.competitors)
  }
}
