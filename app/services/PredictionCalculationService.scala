package services

import com.google.inject.Inject
import models._

import scala.concurrent.Future
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class PredictionCalculationService @Inject()(matchService: MatchService) {

  def statisticalPredictMatch(matchModel: MatchModel): Future[MatchPredictionModel] = {
    //TODO config variables
    val iterations = 100

    def iterate(count: Int = 0, list: Seq[Future[MatchModel]] = Seq.empty): Future[Seq[MatchModel]] = {
      if (count < iterations) iterate(count + 1, list ++ Seq(predictMatch(matchModel)))
      else Future.sequence(list)
    }

    iterate().map { predictions =>
      MatchPredictionModel(matchModel, predictions)
    }
  }

  def predictMatch(matchModel: MatchModel): Future[MatchModel] = {
    for {
      allFirstTeamMatches <- matchService.getAllMatchesForTeam(matchModel.firstTeam)
      allSecondTeamMatches <- matchService.getAllMatchesForTeam(matchModel.secondTeam)
      firstTeamWinRate <- getTotalWinRate(matchModel.firstTeam, matchModel.secondTeam, matchModel.season, matchModel.stage, allFirstTeamMatches)
      secondTeamWinRate <- getTotalWinRate(matchModel.secondTeam, matchModel.firstTeam, matchModel.season, matchModel.stage, allSecondTeamMatches)
    } yield {
      MatchModel(
        matchModel.firstTeam, matchModel.secondTeam, matchModel.time, matchModel.season, matchModel.stage, matchModel.isPlayoff,
        results = Some(getResult(firstTeamWinRate * 100, secondTeamWinRate * 100, matchModel.firstTeam, matchModel.secondTeam))
      )
    }
  }

  def getTotalWinRate(team: String, opposition: String, season: Int, stage: Option[Int], matches: Seq[MatchModel]): Future[BigDecimal] = {

    def calculateIndividualWinRate(matchSubset: Seq[MatchModel]): BigDecimal = {
      if (matchSubset.nonEmpty) {
        matchSubset.count(_.results.exists(_.winner == team)) / matchSubset.size
      } else 0.5
    }

    Future {
      val headToHeadMatches = matches.filter(_.matchingTeams(team, opposition))
      val seasonMatches = matches.filter(_.season == season)
      val stageMatches = seasonMatches.filter(_.stage == stage)

      val totalWinRate = calculateIndividualWinRate(matches)
      val headToHeadWinRate = calculateIndividualWinRate(headToHeadMatches)
      val seasonWinRate = calculateIndividualWinRate(seasonMatches)
      val stageWinRate = calculateIndividualWinRate(stageMatches)

      calculateTotalWinRate(getWeighting(seasonMatches, stageMatches, season), totalWinRate, headToHeadWinRate, seasonWinRate, stageWinRate)
    }
  }

  def calculateTotalWinRate(weighting: WeightModel, totalWinRate: BigDecimal, headToHeadWinRate: BigDecimal, seasonWinRate: BigDecimal, stageWinRate: BigDecimal): BigDecimal = {
    (weighting.totalWeight * totalWinRate +
    weighting.headToHeadWeight * headToHeadWinRate +
    weighting.seasonWeight * seasonWinRate +
    weighting.stageWeight * stageWinRate) / weighting.totalWeight
  }

  def getWeighting(seasonMatches: Seq[MatchModel], stageMatches: Seq[MatchModel], season: Int): WeightModel = {
    //TODO CONFIG VARIABLES
    val totalWeight = 25
    val headToHeadWeight = 50
    val maxSeasonWeight = 50
    val maxStageWeight = 100

    def getRatio(matchSubset: Seq[MatchModel]): BigDecimal = {
      if (matchSubset.nonEmpty) {
        matchSubset.count(_.results.isDefined) / matchSubset.size
      } else 0
    }

    WeightModel(totalWeight, headToHeadWeight, getRatio(seasonMatches) * maxSeasonWeight, getRatio(stageMatches) * maxStageWeight)
  }

  def getResult(firstWR: BigDecimal, secondWR: BigDecimal, firstTeam: String, secondTeam: String): ResultModel = {
    val random = Random.nextInt(((firstWR + secondWR) * 100).toInt)
    val winnerFirst = random <= firstWR*100

    val margin = {
      if (winnerFirst) (firstWR - random) / secondWR
      else (random - firstWR) / firstWR
    }

    val scoreline = {
      if (margin > 0.5) (4, 0)
      else if (margin > 0.25) (3, 1)
      else (3, 2)
    }

    if (winnerFirst) {
      ResultModel(firstTeam, secondTeam, scoreline._1, scoreline._2)
    } else ResultModel(secondTeam, firstTeam, scoreline._1, scoreline._2)
  }
}
