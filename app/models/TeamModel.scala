package models

import play.api.libs.json.{Json, OFormat}

case class TeamModel(name: String, country: String, city: String, identifier: String)

object TeamModel {
  implicit val formats: OFormat[TeamModel] = Json.format[TeamModel]
}
