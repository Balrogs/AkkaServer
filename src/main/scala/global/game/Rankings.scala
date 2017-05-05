package global.game

import argonaut.Argonaut._
import argonaut.EncodeJson
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}

case class Rankings(rank_name:String, player_id: Long, player_name: String, country: Int, rank: Int)


object Rankings {

  implicit def rankingsWriter: BSONDocumentWriter[Rankings] = Macros.writer[Rankings]

  implicit def RankingsEncodeJson: EncodeJson[Rankings] =
    jencode3L((p: Rankings) => (p.player_name, p.country, p.rank))("player_name", "country",  "rank")

  implicit object RankingsReader extends BSONDocumentReader[Rankings] {
    def read(doc: BSONDocument): Rankings = {
      val rank_name = doc.getAs[String]("rank_name").get
      val player_id = doc.getAs[Long]("player_id").get
      val player_name = doc.getAs[String]("player_name").get
      val country = doc.getAs[Int]("country").get
      val rank = doc.getAs[Int]("rank").get
      Rankings(rank_name, player_id, player_name, country, rank)
    }
  }
}