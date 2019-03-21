package models

case class WeightModel(totalWeight: BigDecimal, headToHeadWeight: BigDecimal, seasonWeight: BigDecimal, stageWeight: BigDecimal) {

  val sumWeight: BigDecimal = totalWeight + headToHeadWeight + seasonWeight + stageWeight
}