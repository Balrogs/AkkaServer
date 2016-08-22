package global.game

import argonaut.Argonaut._
import argonaut.EncodeJson
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}

case class Rankings(rank_name: String, player_id: Long, rank: Int) {
  val code = 11
}


object Rankings {

  implicit def rankingsWriter: BSONDocumentWriter[Rankings] = Macros.writer[Rankings]

  implicit def RankingsEncodeJson: EncodeJson[Rankings] =
    jencode4L((p: Rankings) => (p.rank_name, p.player_id, p.rank, p.code))("rank_name", "player_id", "rank", "code")

  implicit object RankingsReader extends BSONDocumentReader[Rankings] {
    def read(doc: BSONDocument): Rankings = {
      val rank_name = doc.getAs[String]("rank_name").get
      val player_id = doc.getAs[Long]("player_id").get
      val rank = doc.getAs[Int]("rank").get
      Rankings(rank_name, player_id, rank)
    }
  }
}