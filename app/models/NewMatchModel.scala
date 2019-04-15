package models

import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class NewMatchModel(id: Int,
                         competitors: Seq[CompetitorModel],
                         status: String,
                         wins: Option[Seq[Int]],
                         ties: Option[Seq[Int]],
                         losses: Option[Seq[Int]]) {
  val isFinished: Boolean = status == "CONCLUDED"

  val components: Seq[MatchComponentModel] = Seq(
    MatchComponentModel(competitors.head, wins.map(_.head).getOrElse(0), ties.map(_.head).getOrElse(0), losses.map(_.head).getOrElse(0)),
    MatchComponentModel(competitors.last, wins.map(_.last).getOrElse(0), ties.map(_.last).getOrElse(0), losses.map(_.last).getOrElse(0))
  )

  lazy val winner: Option[CompetitorModel] = if (isFinished) Some(components.maxBy(_.wins).competitor) else None

  def isMatchForTeam(team: String): Boolean = {
    competitors.exists(_.abbreviatedName.exists(_ == team))
  }

  def areMatchingTeams(teamA: String, teamB: String): Boolean = {
    isMatchForTeam(teamA) && isMatchForTeam(teamB)
  }
}

object NewMatchModel {
  val defaultCompetitor = CompetitorModel(0, "", None, None, None, None)
  implicit val modelReads: Reads[NewMatchModel] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "competitors").read[Seq[CompetitorModel]].orElse(Reads.pure[Seq[CompetitorModel]](Seq(defaultCompetitor, defaultCompetitor))) and
      (JsPath \ "status").read[String] and
      (JsPath \ "wins").readNullable[Seq[Int]] and
      (JsPath \ "ties").readNullable[Seq[Int]] and
      (JsPath \ "losses").readNullable[Seq[Int]]
    )(NewMatchModel.apply _)
  implicit val modelWrites: Writes[NewMatchModel] = Json.writes[NewMatchModel]
}

case class MatchComponentModel(competitor: CompetitorModel,
                               wins: Int,
                               ties: Int,
                               losses: Int) {
  val differential: Int = wins - losses
}

object MatchComponentModel {
  implicit val modelFormats: OFormat[MatchComponentModel] = Json.format[MatchComponentModel]
}

case class TournamentModel(id: String, `type`: String)

object TournamentModel {
  implicit val modelFormats: OFormat[TournamentModel] = Json.format[TournamentModel]
}
