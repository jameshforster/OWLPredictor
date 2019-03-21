package controllers

import models.{DatabaseResponse, DatabaseSuccessResponse}
import play.api.libs.json.{JsError, Reads}
import play.api.mvc.{BaseController, BodyParser, Result}

import scala.concurrent.ExecutionContext.Implicits.global

trait ControllerTemplate extends BaseController {
  def validateJson[A](implicit reads: Reads[A]): BodyParser[A] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e))))

  def checkDatabaseResponses(responses: Seq[DatabaseResponse]): Result =
    if (responses.forall(_.isInstanceOf[DatabaseSuccessResponse[Unit]])) NoContent else BadGateway("Database error")

  def checkDatabaseValues[A](responses: Seq[DatabaseResponse]): Seq[A] = {
    responses.filter(_.isInstanceOf[DatabaseSuccessResponse[A]]).flatMap(_.asInstanceOf[DatabaseSuccessResponse[A]].data)
  }

}
