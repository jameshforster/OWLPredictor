package services

import com.google.inject.{Inject, Singleton}
import connectors.MongoConnector
import models.MatchPredictionModel
import play.api.libs.json.Json
import reactivemongo.api.commands.UpdateWriteResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PredictionService @Inject()(mongoConnector: MongoConnector) {

  val fixedPredictionsCollection = "fixedPredictionsCollection"
  val dynamicPredictionsCollection = "dynamicPredictionsCollection"

  def saveDynamicPredictions(predictions: Seq[MatchPredictionModel]): Future[Seq[UpdateWriteResult]] = {
    Future.sequence(predictions.map { prediction =>
      mongoConnector.saveData(dynamicPredictionsCollection, prediction, Json.obj("matchModel.id" -> prediction.matchModel.id))
    })
  }
}
