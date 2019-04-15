package controllers

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import models.{DatabaseSuccessResponse, MatchModel}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import services.{MatchService, PredictionCalculationService, PredictionService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PredictionController @Inject()(val controllerComponents: ControllerComponents,
                                     matchService: MatchService,
                                     predictionCalculationService: PredictionCalculationService,
                                     predictionService: PredictionService) extends ControllerTemplate {

  val logger = Logger("PredictionController")

  val makePredictions: Action[Seq[String]] = Action(validateJson[Seq[String]]).async { implicit request =>
    Future.sequence(
      request.body.map { predictionRequest =>
        matchService.getMatch(predictionRequest).flatMap {
          case Some(data) => predictionCalculationService.statisticalPredictMatch(data)
          case _ => throw new Exception("MISSING MATCH DATA")
        }
      }
    ).flatMap { response =>
      logger.error(s"PREDICTED WINNER: ${response.head.predictedWinner}")
      logger.error(s"PREDICTED SCORE: ${response.head.predictedScore}")
      logger.error(s"PREDICTED FIRST TEAM WIN RATE: ${response.head.firstTeamWinChance}")
      logger.error(s"PREDICTED SECOND TEAM WIN RATE: ${response.head.secondTeamWinChance}")
      predictionService.saveDynamicPredictions(response).map {
        _ => Ok(Json.toJson(response))
      }
    }
  }

}
