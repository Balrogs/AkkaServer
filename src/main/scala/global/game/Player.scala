package global.game

import argonaut.Argonaut._
import argonaut.{CodecJson, EncodeJson}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}

case class CustomPlayerView(hat: Int, bow:Int, arrow:Int)

object CustomPlayerView{
  implicit def customPlayerViewriter: BSONDocumentWriter[CustomPlayerView] = Macros.writer[CustomPlayerView]

  implicit def customPlayerViewEncodeJson: EncodeJson[CustomPlayerView] =
    jencode3L((p: CustomPlayerView) => (p.hat, p.bow, p.arrow))("hat", "bow", "arrow")

  implicit object customPlayerViewReader extends BSONDocumentReader[CustomPlayerView] {
    def read(doc: BSONDocument): CustomPlayerView = {
      val hat = doc.getAs[Int]("hat").get
      val bow = doc.getAs[Int]("bow").get
      val arrow = doc.getAs[Int]("arrow").get
      CustomPlayerView(hat, bow, arrow)
    }
  }

  implicit def customPlayerViewCodecJson: CodecJson[CustomPlayerView] =
    casecodec3(CustomPlayerView.apply, CustomPlayerView.unapply)("hat", "bow", "arrow")
}

case class Battle(id: Long, player_ids: Array[Long], winner_id: Long)

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

case class Stats(id: Long, battles: Array[Battle], date_reg: String)

object Stats {
  implicit def statsWriter: BSONDocumentWriter[Stats] = Macros.writer[Stats]

  implicit object StatsReader extends BSONDocumentReader[Stats] {
    def read(doc: BSONDocument): Stats = {
      val id = doc.getAs[Long]("id").get
      val battles = doc.getAs[Array[Battle]]("battles").get
      val date_reg = doc.getAs[String]("date_reg").get
      Stats(id, battles, date_reg)
    }
  }

}


case class Player(id: Long, name: String, password: String, rank: Int, country: Int, friends_list: Array[String], playerView: CustomPlayerView)

object Player {

  implicit def playerWriter: BSONDocumentWriter[Player] = Macros.writer[Player]

  implicit object PlayerReader extends BSONDocumentReader[Player] {
    def read(doc: BSONDocument): Player = {
      val id = doc.getAs[Long]("id").get
      val name = doc.getAs[String]("name").get
      val password = doc.getAs[String]("password").get
      val rank = doc.getAs[Int]("rank").get
      val country = doc.getAs[Int]("country").get
      val friends_list = doc.getAs[Array[String]]("friends_list").get
      val playerView = doc.getAs[CustomPlayerView]("playerView").get
      Player(id, name, password, rank, country, friends_list, playerView)
    }
  }

}