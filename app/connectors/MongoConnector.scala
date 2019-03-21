package connectors

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import play.modules.reactivemongo.{ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.Cursor
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MongoConnector @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends ReactiveMongoComponents {

  private def getCollection(name: String): Future[JSONCollection] = {
    reactiveMongoApi.database.map(_.collection[JSONCollection](name))
  }

  def saveData[A](collectionName: String, data: A, selector: JsObject)(implicit writes: Writes[A]): Future[UpdateWriteResult] = {
    getCollection(collectionName).flatMap(_.update.one(selector, Json.toJson(data).asInstanceOf[JsObject], true))
  }

  def deleteData(collectionName: String, selector: JsObject): Future[WriteResult] = {
    getCollection(collectionName).flatMap(_.delete.one(selector))
  }

  def getData[A](collectionName: String, selector: JsObject)(implicit reads: Reads[A]): Future[Option[A]] = {
    getCollection(collectionName).flatMap(_.find[JsObject, JsObject](selector, None).one[A])
  }

  def getAllData[A](collectionName: String, selector: JsObject)(implicit reads: Reads[A]): Future[Seq[A]] = {
    getCollection(collectionName).flatMap(
      _.find[JsObject, JsObject](selector, None).cursor[A]().collect[Seq](-1, Cursor.ContOnError[Seq[A]]())
    )
  }
}
