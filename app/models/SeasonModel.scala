package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SeasonModel(id: String, stages: Seq[StageModel]) {
  def allMatches: Seq[NewMatchModel] = stages.flatMap(_.matches)
  def filterByTeam(team: String): SeasonModel = SeasonModel(id, stages.map(_.filterByTeam(team)))
  def getMatchStage(id: Int): Option[String] = stages.find(_.matches.exists(_.id == id)).map(_.stage)
}

object SeasonModel {
  implicit val modelReads: Reads[SeasonModel] = (
    (JsPath \ "data" \ "id").read[String] and
      (JsPath \ "data"  \ "stages").read[Seq[StageModel]]
  )(SeasonModel.apply _)

  implicit val modelWrites: Writes[SeasonModel] = Json.writes[SeasonModel]
}

case class StageModel(name: String, matches: Seq[NewMatchModel]) {
  val stage: String = name.replace("Stage ", "")
  def allMatchesForTeam(team: String): Seq[NewMatchModel] = matches.filter(_.isMatchForTeam(team))
  def filterByTeam(team: String): StageModel = StageModel(name, allMatchesForTeam(team))
}

object StageModel {
  implicit val modelFormats: OFormat[StageModel] = Json.format[StageModel]
}
