package global.game

import argonaut.Argonaut._
import argonaut.EncodeJson
import reactivemongo.bson._

case class EventInfo(e_type:String, prizes:Array[String])

object EventInfo {

  implicit def eventInfoWriter: BSONDocumentWriter[EventInfo] = Macros.writer[EventInfo]

  implicit def eventInfoEncodeJson: EncodeJson[EventInfo] =
    jencode2L((p: EventInfo) => (p.e_type, p.prizes))("e_type", "prizes")

  implicit object EventInfoReader extends BSONDocumentReader[EventInfo] {
    def read(doc: BSONDocument): EventInfo = {
      val e_type = doc.getAs[String]("e_type").get
      val prizes = doc.getAs[Array[String]]("prizes").get
      EventInfo(e_type, prizes)
    }
  }
}

case class GameEvent(id: Long, name: String, date_begin: String, date_end: String, description: EventInfo,isAwardsSet:Boolean) {
  val code = 13
}

object GameEvent {

  implicit def gameEventWriter: BSONDocumentWriter[GameEvent] = Macros.writer[GameEvent]

  implicit def gameEventEncodeJson: EncodeJson[GameEvent] =
    jencode6L((p: GameEvent) => (p.id, p.name, p.date_begin, p.date_end, p.description, p.code))("id", "name", "date_begin", "date_end","description", "code")

  implicit object GameEventReader extends BSONDocumentReader[GameEvent] {
    def read(doc: BSONDocument): GameEvent = {
      val id = doc.getAs[Long]("id").get
      val name = doc.getAs[String]("name").get
      val date_begin = doc.getAs[String]("date_begin").get
      val date_end = doc.getAs[String]("date_end").get
      val description = doc.getAs[EventInfo]("description").get
      val isAwardsSet = doc.getAs[Boolean]("isAwardsSet").get
      GameEvent(id, name, date_begin, date_end, description, isAwardsSet)
    }

  }
}

