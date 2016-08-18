package global

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, Macros, BSONDocumentWriter}

case class Player(id: Long, name: String, password: String, var rank: Int, friends_list: Array[Int])

object Player {

  implicit def playerWriter: BSONDocumentWriter[Player] = Macros.writer[Player]

  implicit object PlayerReader extends BSONDocumentReader[Player] {
    def read(doc: BSONDocument): Player = {
      val id = doc.getAs[Long]("id").get
      val name = doc.getAs[String]("name").get
      val password = doc.getAs[String]("password").get
      val rank = doc.getAs[Int]("rank").get
      val friends_list = doc.getAs[Array[Int]]("friends_list").get
      Player(id, name, password, rank, friends_list)
    }
  }

}