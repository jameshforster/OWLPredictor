package models

case class MatchPredictionModel(matchModel: MatchModel, predictions: Seq[MatchModel]) {

  val firstTeamWinChance: BigDecimal = BigDecimal(predictions.count(_.results.exists(_.winner == matchModel.firstTeam))) / predictions.size
  val secondTeamWinChance: BigDecimal = 1 - firstTeamWinChance
  val predictedWinner: String = if (firstTeamWinChance > secondTeamWinChance) matchModel.firstTeam else matchModel.secondTeam
  val sortedPredictions: Seq[((Int, Int), Int)] = predictions.filter(_.results.exists(_.winner == predictedWinner)).groupBy { prediction =>
    (prediction.results.map(_.winnerScore), prediction.results.map(_.loserScore))
  }.map(x => ((x._1._1.getOrElse(0), x._1._2.getOrElse(0)), x._2.size)).toSeq
  val predictedScore: (Int, Int) = sortedPredictions.maxBy(_._2)._1
  val scoreChance: BigDecimal = BigDecimal(sortedPredictions.maxBy(_._2)._2) / predictions.count(_.results.exists(_.winner == predictedWinner))
}
