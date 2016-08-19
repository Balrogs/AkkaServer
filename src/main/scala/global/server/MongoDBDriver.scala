package global.server

import java.time.LocalDateTime

import global.game._
import reactivemongo.api.{DefaultDB, MongoConnection}
import reactivemongo.bson._

import scala.concurrent.Future

object MongoDBDriver {


  private val mongoUri = "mongodb://127.0.0.1:27017/arcunlimdb"

  import scala.concurrent.ExecutionContext.Implicits.global

  // use any appropriate context

  val driver = new reactivemongo.api.MongoDriver
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future fromTry connection

  var db: Future[DefaultDB] = futureConnection.flatMap(_.database("arcunlimdb"))

  def playerCollection = db.map(_.collection("players"))

  def statsCollection = db.map(_.collection("statistics"))

  def battlesCollection = db.map(_.collection("battles"))

  def eventsCollection = db.map(_.collection("events"))

  def rankingsCollection = db.map(_.collection("rankings"))

  def awardsCollection = db.map(_.collection("awards"))

  def createPlayer(player: Player): Future[Unit] = {
    playerCollection.flatMap(_.insert(player).map(_ => {}))
    createStats(Stats(player.id, Array.empty[Battle], LocalDateTime.now().toString))
    createRankings(Rankings("global",player.id, player.rank))
    createRankings(Rankings("country-" + player.country,player.id, player.rank))
  }

  def createStats(stats: Stats): Future[Unit] = statsCollection.flatMap(_.insert(stats).map(_ => {}))

  def createRankings(rankings: Rankings): Future[Unit] = rankingsCollection.flatMap(_.insert(rankings).map(_ => {}))

  def createAward(award: Award): Future[Unit] = awardsCollection.flatMap(_.insert(award).map(_ => {}))

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

  def updateRankings(rankings: Rankings): Future[Int] = {
    val selector = document(
      "player_id" -> rankings.player_id,
      "rank_name" -> rankings.rank_name
    )
    rankingsCollection.flatMap(_.update(selector, rankings).map(_.n))
  }


  def updateGameEvent(event: GameEvent): Future[Int] = {
    val selector = document(
      "id" -> event.id
    )
    eventsCollection.flatMap(_.update(selector, event).map(_.n))
  }

  def updateAward(award: Award): Future[Int] = {
    val selector = document(
      "id" -> award.id,
      "received" -> false
    )
    awardsCollection.flatMap(_.update(selector, award).map(_.n))
  }


  def getLastId(id: Long): Future[Int] = {
    id match {
      case 1 => playerCollection.flatMap(_.count())
      case 2 => awardsCollection.flatMap(_.count())
      case 3 => battlesCollection.flatMap(_.count())
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

  def AuthPlayer(id: Long, pass: String): Future[Option[Player]] = {
    val query = BSONDocument("id" -> id, "password" -> pass)
    playerCollection.flatMap(_.find(query).one[Player])
  }

  def findStatsById(id: Long): Future[Option[Stats]] = {
    val query = BSONDocument("id" -> id)

    statsCollection.flatMap(_.find(query).one[Stats])
  }

  def findRankingsByName(name: String): Future[Array[Rankings]] =
    rankingsCollection.flatMap(_.find(document("rank_name" -> name)).cursor[Rankings]().collect[Array]())


  def findPlayerRankings(id: Long, rank_name : String): Future[Option[Rankings]] = {
    val query = BSONDocument("player_id" -> id, "rank_name" -> rank_name)

    rankingsCollection.flatMap(_.find(query).one[Rankings])
  }

  def findGameEvents(): Future[Array[GameEvent]] = {
    eventsCollection.flatMap(_.find(document()).cursor[GameEvent]().collect[Array]())
  }

  def findEventById(id:Long): Future[Option[GameEvent]] = {
    val query = BSONDocument("id" -> id)

    eventsCollection.flatMap(_.find(query).one[GameEvent])
  }

  def findAwardByPlayerId(player_id:Long): Future[Array[Award]] = {
    val query = BSONDocument("player_id" -> player_id, "received" -> false)

    awardsCollection.flatMap(_.find(query).cursor[Award]().collect[Array]())
  }
}