package models

import java.time.LocalDateTime

import play.api.libs.json.{Json, OFormat}

case class MatchModel(firstTeam: String,
                      secondTeam: String,
                      time: LocalDateTime,
                      season: Int,
                      stage: Option[Int],
                      isPlayoff: Boolean,
                      results: Option[ResultModel]) {
  val isFinished: Boolean = results.isDefined

  def matchingTeams(teamA: String, teamB: String): Boolean = {
    (teamA == firstTeam && teamB == secondTeam) || (teamA == secondTeam && teamB == firstTeam)
  }
}

object MatchModel {
  implicit val formats: OFormat[MatchModel] = Json.format[MatchModel]
}
