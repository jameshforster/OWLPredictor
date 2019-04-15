package models

import play.api.libs.json.{Json, OFormat}

case class PredictionModel(components: Seq[MatchComponentModel]) {
  val predictedWinner: CompetitorModel = components.maxBy(_.wins).competitor
}

object PredictionModel {
  implicit val modelFormats: OFormat[PredictionModel] = Json.format[PredictionModel]
}
