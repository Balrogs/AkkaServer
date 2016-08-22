package global.game

import argonaut.Argonaut._
import argonaut.EncodeJson
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}


case class Award(id: Long, player_id: Long, prize: String, received: Boolean) {
  val code = 15
}

object Award {

  implicit def awardWriter: BSONDocumentWriter[Award] = Macros.writer[Award]

  implicit def awardEncodeJson: EncodeJson[Award] =
    jencode5L((p: Award) => (p.id, p.player_id, p.prize, p.received, p.code))("id", "player_id", "prize", "received", "code")

  implicit object awardReader extends BSONDocumentReader[Award] {
    def read(doc: BSONDocument): Award = {
      val id = doc.getAs[Long]("id").get
      val player_id = doc.getAs[Long]("player_id").get
      val prize = doc.getAs[String]("prize").get
      val received = doc.getAs[Boolean]("received").get
      Award(id, player_id, prize, received)
    }
  }
}
