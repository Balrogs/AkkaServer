package global

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, Macros, BSONDocumentWriter}


case class Stats(id:Long, battles_ids: Array[Long], date_reg:String)

object Stats {
  implicit def statsWriter: BSONDocumentWriter[Stats] = Macros.writer[Stats]
  implicit object StatsReader extends BSONDocumentReader[Stats] {
    def read(doc: BSONDocument): Stats = {
      val id = doc.getAs[Long]("id").get
      val battles_ids = doc.getAs[Array[Long]]("battles_ids").get
      val date_reg = doc.getAs[String]("date_reg").get
      Stats(id, battles_ids, date_reg)
    }
  }

}

case class Battle(id:Long, player_ids:Array[Long], winner_id:Long)

object Battle {

  implicit def battleWriter: BSONDocumentWriter[Battle] = Macros.writer[Battle]

  implicit object BattleReader extends BSONDocumentReader[Battle] {
    def read(doc: BSONDocument): Battle = {
      val id = doc.getAs[Long]("id").get
      val player_ids = doc.getAs[Array[Long]]("player_ids").get
      val winner_id = doc.getAs[Long]("winner_id").get
      Battle(id, player_ids, winner_id)
    }
  }

}