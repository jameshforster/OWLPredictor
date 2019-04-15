package models

import play.api.libs.json.{Json, OFormat}

case class MatchPredictionModel(matchModel: NewMatchModel, predictions: Seq[PredictionModel]) {

  val firstTeamWinChance: BigDecimal = BigDecimal(predictions.count(_.predictedWinner == matchModel.competitors.head)) / predictions.size
  val secondTeamWinChance: BigDecimal = 1 - firstTeamWinChance
  val predictedWinner: CompetitorModel = if (firstTeamWinChance > secondTeamWinChance) matchModel.competitors.head else matchModel.competitors.last
  val sortedPredictions: Seq[((Int, Int), Int)] = predictions.filter(_.predictedWinner == predictedWinner).groupBy { prediction =>
    (prediction.components.head.wins, prediction.components.head.losses)
  }.map(x => ((x._1._1, x._1._2), x._2.size)).toSeq
  val predictedScore: (Int, Int) = sortedPredictions.maxBy(_._2)._1
  val scoreChance: BigDecimal = BigDecimal(sortedPredictions.maxBy(_._2)._2) / predictions.count(_.predictedWinner == predictedWinner)
}

object MatchPredictionModel {
  implicit val formats: OFormat[MatchPredictionModel] = Json.format[MatchPredictionModel]
}
