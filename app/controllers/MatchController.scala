package controllers

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import models.MatchModel
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import services.MatchService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MatchController @Inject()(val controllerComponents: ControllerComponents, matchService: MatchService) extends ControllerTemplate {

  val uploadMatches: Action[Seq[MatchModel]] = Action(validateJson[Seq[MatchModel]]).async { implicit request =>
    Future.sequence(request.body.map(model => matchService.saveMatch(model))).map(checkDatabaseResponses)
  }

  val deleteMatches: Action[Seq[LocalDateTime]] = Action(validateJson[Seq[LocalDateTime]]).async { implicit request =>
    Future.sequence(request.body.map(time => matchService.deleteMatch(time))).map(checkDatabaseResponses)
  }

  val getMatches: Action[Seq[LocalDateTime]] = Action(validateJson[Seq[LocalDateTime]]).async { implicit request =>
    Future.sequence(request.body.map(time => matchService.getMatch(time))).map { matches =>
      Ok(Json.toJson(checkDatabaseValues[MatchModel](matches)))
    }
  }
}
