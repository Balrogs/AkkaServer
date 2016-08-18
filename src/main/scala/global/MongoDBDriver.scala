package global


import java.time.{LocalDateTime, Clock}

import reactivemongo.api.{ DefaultDB, MongoConnection}
import reactivemongo.bson._

import scala.concurrent.Future
import java.time

object MongoDBDriver {


  private val mongoUri = "mongodb://127.0.0.1:27017/arcunlimdb"

  import scala.concurrent.ExecutionContext.Implicits.global // use any appropriate context

  val driver = new reactivemongo.api.MongoDriver
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future fromTry connection

  var db: Future[DefaultDB] = futureConnection.flatMap(_.database("arcunlimdb"))

  def playerCollection = db.map(_.collection("players"))

  def statsCollection = db.map(_.collection("statistics"))

  def battlesCollection = db.map(_.collection("battles"))

  def createPlayer(player: Player): Future[Unit] ={
    playerCollection.flatMap(_.insert(player).map(_ => {}))
    createStats(Stats(player.id,Array.empty[Long],LocalDateTime.now().toString))
  }

  def createStats(stats: Stats): Future[Unit] =
    statsCollection.flatMap(_.insert(stats).map(_ => {}))

  def createBattle(battle: Battle): Future[Unit] = battlesCollection.flatMap(_.insert(battle).map(_ => {}))



  def updatePlayer(player: Player): Future[Int] = {
    val selector = document(
    "id" -> player.id
    )
    playerCollection.flatMap(_.update(selector, player).map(_.n))
  }

  def updateStats(stats: Stats): Future[Int] = {
    val selector = document(
      "id" -> stats.id
    )
    statsCollection.flatMap(_.update(selector, stats).map(_.n))
  }

  def getLastId(id:Long): Future[Int] = {
    id match{
      case 1 =>  playerCollection.flatMap(_.count())
      case 3=>   battlesCollection.flatMap(_.count())
    }

  }

  def playerInfo(id: Long): Future[Option[Player]] = {
    val query = BSONDocument("id" -> id)

    playerCollection.flatMap(_.find(query).one[Player])
  }

  def playerInfoByName(name: String): Future[Option[Player]] = {
    val query = BSONDocument("name" -> name)

    playerCollection.flatMap(_.find(query).one[Player])
  }

  def AuthPlayer(id: Long, pass:String): Future[Option[Player]] = {
    val query = BSONDocument("id" -> id,"password" -> pass)
    playerCollection.flatMap(_.find(query).one[Player])
  }

  def findStatsById(id: Long): Future[Option[Stats]] = {
    val query = BSONDocument("id" -> id)

    statsCollection.flatMap(_.find(query).one[Stats])
  }

  def findBattleById(id: Long): Future[Option[Battle]] = {
    val query = BSONDocument("id" -> id)

    battlesCollection.flatMap(_.find(query).one[Battle])
  }
}