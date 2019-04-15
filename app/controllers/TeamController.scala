package controllers

import com.google.inject.{Inject, Singleton}
import models.TeamModel
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.TeamService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TeamController @Inject()(val controllerComponents: ControllerComponents, teamService: TeamService) extends ControllerTemplate {

  val logger = Logger("[Team Controller]")

  val uploadTeams: Action[Seq[TeamModel]] = Action(validateJson[Seq[TeamModel]]).async { implicit request =>
    Future.sequence(request.body.map(teamService.saveTeam)).map(checkDatabaseResponses)
  }

  val getAllTeams: Action[AnyContent] = Action.async { implicit request =>
    teamService.getAllTeams.map { teams =>
      logger.error(s"TEAMS: $teams")
      Ok(Json.toJson(teams.map(_.competitor)))
    }
  }
}
