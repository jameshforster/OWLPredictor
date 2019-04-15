package services

import com.google.inject.Inject
import models._
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class PredictionCalculationService @Inject()(matchService: MatchService) {
  val logger = Logger("[PredictionService]")

  def statisticalPredictMatch(matchModel: NewMatchModel): Future[MatchPredictionModel] = {
    //TODO config variables
    val iterations = 1000
    matchService.getAllMatches.map { seasons =>
      val season = seasons.find(_.getMatchStage(matchModel.id).isDefined).get
      val stage = season.getMatchStage(matchModel.id)
      val firstTeamWinRate = getTotalWinRate(matchModel.competitors.head.abbreviatedName.get, matchModel.competitors.last.abbreviatedName.get, season.id, stage, seasons)
      val secondTeamWinRate = getTotalWinRate(matchModel.competitors.last.abbreviatedName.get, matchModel.competitors.head.abbreviatedName.get, season.id, stage, seasons)

      def iterate(count: Int = 0, list: Seq[PredictionModel] = Seq.empty): Seq[PredictionModel] = {
        if (count < iterations) iterate(count + 1, list ++ Seq(predictMatch(matchModel, firstTeamWinRate, secondTeamWinRate)))
        else list
      }

      logger.error(s"FIRST TEAM WIN RATE: $firstTeamWinRate")
      logger.error(s"SECOND TEAM WIN RATE: $secondTeamWinRate")

      MatchPredictionModel(matchModel, iterate())
    }
  }

  def fixedStatisticalPredictMatch(season: String, stage: Int): Future[Seq[MatchPredictionModel]] = {
    matchService.getMatchesForStage(season, stage).flatMap { stageModel =>
      if (stageModel.exists(_.matches.forall(_.isFinished))) {
        Future.sequence(stageModel.get.matches.map(statisticalPredictMatch))
      } else {
        logger.error("Match results already exist, cannot make a Fixed Prediction")
        Future.successful(Seq.empty[MatchPredictionModel])
      }
    }
  }

  def predictMatch(matchModel: NewMatchModel, firstTeamWinRate: BigDecimal, secondTeamWinRate: BigDecimal): PredictionModel = {
    PredictionModel(getResult(firstTeamWinRate * 100, secondTeamWinRate * 100, matchModel.competitors.head, matchModel.competitors.last))
  }

  def getTotalWinRate(team: String, opposition: String, season: String, stage: Option[String], seasons: Seq[SeasonModel]): BigDecimal = {

    def calculateIndividualWinRate(matchSubset: Seq[NewMatchModel]): BigDecimal = {
      if (matchSubset.count(_.isFinished) > 0) {
        BigDecimal(matchSubset.count(_.winner.exists(_.abbreviatedName.get == team))) / BigDecimal(matchSubset.count(_.isFinished))
      } else 0.5
    }

    val headToHeadMatches = seasons.flatMap(_.allMatches).filter(_.areMatchingTeams(team, opposition))
    val seasonMatches = seasons.find(_.id == season).map(_.allMatches.filter(_.isMatchForTeam(team))).getOrElse(Seq())
    val stageMatches = seasons.find(_.id == season).flatMap(_.stages.find(_.stage == stage.getOrElse(0)).map(_.matches.filter(_.isMatchForTeam(team)))).getOrElse(Seq())

    val totalWinRate = calculateIndividualWinRate(seasons.flatMap(_.filterByTeam(team).allMatches))
    val headToHeadWinRate = calculateIndividualWinRate(headToHeadMatches)
    val seasonWinRate = calculateIndividualWinRate(seasonMatches)
    val stageWinRate = calculateIndividualWinRate(stageMatches)

    logger.error(s"$team")
    logger.error(s"TOTAL:$totalWinRate, H2H:$headToHeadWinRate, SEASON:$seasonWinRate, STAGE:$stageWinRate")

    calculateTotalWinRate(getWeighting(seasonMatches, stageMatches, headToHeadMatches), totalWinRate, headToHeadWinRate, seasonWinRate, stageWinRate)
  }

  def calculateTotalWinRate(weighting: WeightModel, totalWinRate: BigDecimal, headToHeadWinRate: BigDecimal, seasonWinRate: BigDecimal, stageWinRate: BigDecimal): BigDecimal = {
    val result = (weighting.totalWeight * totalWinRate +
      weighting.headToHeadWeight * headToHeadWinRate +
      weighting.seasonWeight * seasonWinRate +
      weighting.stageWeight * stageWinRate) / weighting.sumWeight

    if (result < 0.05) 0.05 else result
  }

  def getWeighting(seasonMatches: Seq[NewMatchModel], stageMatches: Seq[NewMatchModel], headToHeadMatches: Seq[NewMatchModel]): WeightModel = {
    //TODO CONFIG VARIABLES
    val totalWeight = 60
    val headToHeadWeight: BigDecimal = {
      val count = headToHeadMatches.count(_.isFinished)
      val base = BigDecimal(150)

      if (count <= 0) 0
      else if (count >= 10) base
      else base * count / 10
    }

    val maxSeasonWeight = 120
    val maxStageWeight = 210

    def getRatio(matchSubset: Seq[NewMatchModel]): BigDecimal = {
      if (matchSubset.nonEmpty) {
        BigDecimal(matchSubset.count(_.isFinished)) / BigDecimal(matchSubset.size)
      } else 0
    }

    WeightModel(totalWeight, headToHeadWeight, getRatio(seasonMatches) * maxSeasonWeight, getRatio(stageMatches) * maxStageWeight)
  }

  def getResult(firstWR: BigDecimal, secondWR: BigDecimal, firstTeam: CompetitorModel, secondTeam: CompetitorModel): Seq[MatchComponentModel] = {
    val random = Random.nextInt(((firstWR + secondWR) * 100).toInt)

    val winnerFirst = random <= firstWR * 100

    val margin = {
      if (winnerFirst) (firstWR * 100 - random) / (secondWR * 100)
      else (random - firstWR * 100) / (firstWR * 100)
    }

    val scoreline = {
      if (margin > 1.5) (4, 0, 0)
      else if (margin > 1.3) (3, 1, 0)
      else if (margin > 0.7) (3, 0, 1)
      else if (margin > 0.4) (2, 1, 1)
      else (3, 0, 2)
    }

    val firstTeamComponent =
      if (winnerFirst) MatchComponentModel(firstTeam, scoreline._1, scoreline._2, scoreline._3)
      else MatchComponentModel(firstTeam, scoreline._3, scoreline._2, scoreline._1)

    val secondTeamComponent =
      if (!winnerFirst) MatchComponentModel(secondTeam, scoreline._1, scoreline._2, scoreline._3)
      else MatchComponentModel(secondTeam, scoreline._3, scoreline._2, scoreline._1)

    Seq(firstTeamComponent, secondTeamComponent)
  }
}
