package models

import play.api.libs.json.{Json, OFormat}

case class ResultModel(winner: String, loser: String, winnerScore: Int, loserScore: Int) {
  val mapDifferential: Int = winnerScore - loserScore
}

object ResultModel {
  implicit val formats: OFormat[ResultModel] = Json.format[ResultModel]
}
