package models

import play.api.libs.json._

case class CompetitorModel(id: Int, name: String, abbreviatedName: Option[String], addressCountry: Option[String], homeLocation: Option[String], icon: Option[String])

case class CompetitorWithDivisionModel(competitor: CompetitorModel)

case class CompetitorsModel(competitors: Seq[CompetitorWithDivisionModel])

object CompetitorsModel {
  implicit val modelFormats: OFormat[CompetitorsModel] = Json.format[CompetitorsModel]
}

object CompetitorWithDivisionModel {
  implicit val modelFormats: OFormat[CompetitorWithDivisionModel] = Json.format[CompetitorWithDivisionModel]
}

object CompetitorModel {
  implicit val modelOptFormat: Format[Option[CompetitorModel]] = Format.optionWithNull[CompetitorModel]

  implicit val modelFormats: OFormat[CompetitorModel] = Json.format[CompetitorModel]
}