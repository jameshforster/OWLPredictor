package controllers

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import models.{MatchModel, SeasonModel}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.MatchService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MatchController @Inject()(val controllerComponents: ControllerComponents, matchService: MatchService) extends ControllerTemplate {

  val uploadMatches: Action[Seq[MatchModel]] = Action(validateJson[Seq[MatchModel]]).async { implicit request =>
    Future.sequence(request.body.map(matchService.saveMatch)).map(checkDatabaseResponses)
  }

  val deleteMatches: Action[Seq[LocalDateTime]] = Action(validateJson[Seq[LocalDateTime]]).async { implicit request =>
    Future.sequence(request.body.map(matchService.deleteMatch)).map(checkDatabaseResponses)
  }

  val getMatches: Action[Seq[String]] = Action(validateJson[Seq[String]]).async { implicit request =>
    Future.sequence(request.body.map(matchService.getMatch)).map { matches =>
      Ok(Json.toJson(matches))
    }
  }

  def getMatchesByStageAndSeason(season: String, stage: Option[Int]): Action[AnyContent] = Action.async { implicit request =>
    val x = stage match {
      case Some(data) => matchService.getMatchesForStage(season, data).map(stage => SeasonModel(season, stage.toSeq))
      case _ => matchService.getMatchesForSeason(season)
    }

    x.map { matches =>
      Ok(Json.toJson(matches))
    }
  }

  val getAllMatches: Action[AnyContent] = Action.async { implicit request =>
    matchService.getAllMatches.map { matches =>
      Ok(Json.toJson(matches))
    }
  }
}
