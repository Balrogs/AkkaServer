package global.game

import argonaut.Argonaut._
import argonaut.EncodeJson
import reactivemongo.bson._

case class EventInfo(e_type: Int, infos: Array[Int], rewards: Array[Int])

object EventInfo {

  implicit def eventInfoWriter: BSONDocumentWriter[EventInfo] = Macros.writer[EventInfo]

  implicit def eventInfoEncodeJson: EncodeJson[EventInfo] =
    jencode3L((p: EventInfo) => (p.e_type, p.infos, p.rewards))("e_type", "infos", "rewards")

  implicit object EventInfoReader extends BSONDocumentReader[EventInfo] {
    def read(doc: BSONDocument): EventInfo = {
      val e_type = doc.getAs[Int]("e_type").get
      val infos = doc.getAs[Array[Int]]("infos").get
      val rewards = doc.getAs[Array[Int]]("rewards").get
      EventInfo(e_type, infos, rewards)
    }
  }
}

case class GameEvent(id: Long, date_begin: String, date_end: String, description: EventInfo, isAwardsSet:Boolean) {
  val code = 13
}

object GameEvent {

  implicit def gameEventWriter: BSONDocumentWriter[GameEvent] = Macros.writer[GameEvent]

  implicit def gameEventEncodeJson: EncodeJson[GameEvent] =
    jencode5L((p: GameEvent) => (p.id, p.date_begin, p.date_end, p.description, p.code))("id", "date_begin", "date_end","description", "code")

  implicit object GameEventReader extends BSONDocumentReader[GameEvent] {
    def read(doc: BSONDocument): GameEvent = {
      val id = doc.getAs[Long]("id").get
      val date_begin = doc.getAs[String]("date_begin").get
      val date_end = doc.getAs[String]("date_end").get
      val description = doc.getAs[EventInfo]("description").get
      val isAwardsSet = doc.getAs[Boolean]("isAwardsSet").get
      GameEvent(id, date_begin, date_end, description, isAwardsSet)
    }

  }
}

