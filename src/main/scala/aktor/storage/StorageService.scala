package aktor.storage

import java.time.LocalDateTime

import akka.actor.{ActorRef, Actor, ActorLogging}
import aktor.gm.GameService.JoinLobby
import argonaut._, Argonaut._
import global.game._
import global.server._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class StorageService extends Actor with ActorLogging {

  import StorageService._

  val gameService = context.actorSelection("/user/game")

  override def preStart() {
    log.info("Starting storage service")
  }

  override def receive = {
    case task: StorageLogin =>
      authUser(task)
      context stop self

    case task: StorageRegister =>
      registerUser(task)
      context stop self

    case task: StorageGameOver =>
      finalizeGame(task)
      context stop self

    case task: StorageStats =>
      returnStats(task)
      context stop self

    case task: StorageAddFriends =>
      addToFriends(task)
      context stop self

    case task: StorageAddEventScore =>
      addEventScore(task)
      context stop self

    case StorageEvent =>
      updateEvent()
      context stop self

    case _ =>
      log.info("unknown message " + self.path.name)
  }

  override def postStop() {

    log.info("Stopping storage service")

  }

  def authUser(task: StorageLogin): Unit = {

    val player = MongoDBDriver.AuthPlayer(task.event.id, task.event.password)
    player onSuccess {
      case p => p match {
        case Some(pl) =>
          task.session ! ServerResp(AuthResp(pl.id).code).asJson
          sendPlayerAward(task.session, pl)
          gameService ! JoinLobby(task.session, pl, None)
        case None =>
          task.session ! ServerResp(LoginError.code).asJson
      }
    }
    player onFailure {
      case _ => task.session ! ServerResp(ServerConnectionError.code).asJson
    }

  }

  def registerUser(task: StorageRegister): Unit = {

    MongoDBDriver.playerInfoByName(task.event.name) onSuccess {
      case pl => pl match {
        case Some(player) =>
          task.session ! ServerResp(RegisterErrorNameExists.code).asJson
        case None =>
          MongoDBDriver.getLastId(1) onSuccess {
            case id =>
              MongoDBDriver.createPlayer(Player(
                id,
                task.event.name,
                task.event.password,
                1,
                task.event.country,
                Array.empty[Long]
              )) onSuccess {
                case _ => task.session ! ServerResp(AuthResp(id).code).asJson
              }
          }
      }
    }
  }

  def finalizeGame(task: StorageGameOver): Unit = {

    MongoDBDriver.getLastId(3) onSuccess {
      case id =>

        val battle = Battle(id.toLong, task.players_ids.keys.toArray[Long], task.winner_id)

        task.players_ids.foreach(p => {

          MongoDBDriver.playerInfo(p._1) onSuccess {
            case player => player match {
              case Some(pl) =>
                MongoDBDriver.updatePlayer(Player(pl.id, pl.name, pl.password, pl.rank + p._2, pl.country, pl.friends_list))

                MongoDBDriver.findPlayerRankings(pl.id, "global") onSuccess {
                  case rankings => rankings match {
                    case Some(rank) =>
                      MongoDBDriver.updateRankings(Rankings("global", pl.id, pl.rank + p._2))
                    case None =>
                  }
                }


                MongoDBDriver.findPlayerRankings(pl.id, "country " + pl.country) onSuccess {
                  case rankings => rankings match {
                    case Some(rank) =>
                      MongoDBDriver.updateRankings(Rankings("country " + pl.country, pl.id, pl.rank + p._2))
                    case None =>
                  }
                }

              case None =>
            }
          }

          MongoDBDriver.findStatsById(p._1) onSuccess {
            case stats => stats match {
              case Some(s) =>
                val battles = ArrayBuffer[Battle](battle)
                s.battles.foreach(b => battles += b)
                log.info(battles.toArray.toString)
                MongoDBDriver.updateStats(Stats(p._1, battles.toArray, s.date_reg))
              case None =>
            }
          }
        })
    }

  }

  def returnStats(task: StorageStats): Unit = {

    task.event.s_type match {

      case 1 =>

        MongoDBDriver.findRankingsByName(task.event.name) onSuccess {
          case r_list =>
            task.session ! RankingsRequest(task.event.name, r_list.sortWith((a, b) =>
              if (a.rank > b.rank) true
              else false
            )).asJson
        }

      case 2 =>

        MongoDBDriver.findRankingsByName(task.event.name) onSuccess {
          case r_list =>
            task.session ! RankingsRequest(task.event.name, r_list.sortWith((a, b) =>
              if (a.rank > b.rank) true
              else false
            )).asJson
        }

      case 3 =>

        MongoDBDriver.playerInfoByName(task.event.name) onSuccess {
          case pl => pl match {
            case Some(player) =>
              MongoDBDriver.findStatsById(player.id) onSuccess {
                case st => st match {
                  case Some(stats) =>
                    val battles_win = stats.battles.count(b => {
                      b.winner_id == stats.id
                    })

                    val global_rankings = Await.result(MongoDBDriver.findRankingsByName("global"), 100 milliseconds).map(e => (e.player_id, e.rank))

                    val player_global_rank = global_rankings.find(p => p._1 == player.id).get._2

                    val global_rank = global_rankings.count(rank => rank._2 > player_global_rank)

                    val country_rankings = Await.result(MongoDBDriver.findRankingsByName("country-" + player.country), 100 milliseconds).map(e => (e.player_id, e.rank))

                    val player_country_rank = country_rankings.find(p => p._1 == player.id).get._2

                    val country_rank = country_rankings.count(rank => rank._2 > player_country_rank)

                    task.session ! PlayersStats(player.name, player.rank, global_rank, country_rank, stats.battles.size, battles_win, stats.battles.size - battles_win, stats.date_reg).asJson
                  case None => task.session ! ServerResp(ServerConnectionError.code).asJson
                }
              }
            case None =>

              task.session ! ServerResp(UserNotFound.code).asJson
          }
        }
    }
  }

  def updateEvent() = {
    MongoDBDriver.findGameEvents() onSuccess {
      case list => list.find(event => {
        val localTime = LocalDateTime.now()
        if (LocalDateTime.parse(event.date_begin).isBefore(localTime) && LocalDateTime.parse(event.date_end).isAfter(localTime))
          true
        else
          false
      }) match {
        case Some(event) =>
          determineAwards(event.id - 1)
          gameService ! event
        case None =>
      }
    }
  }

  def determineAwards(event_id: Long): Unit = {
    MongoDBDriver.findEventById(event_id) onSuccess {
      case e => e match {
        case Some(event) =>
          val players_map = Await.result(MongoDBDriver.findRankingsByName("event-" + event_id), 100 milliseconds).map(rankings => (rankings.player_id, rankings.rank))
          var last_award_id = Await.result(MongoDBDriver.getLastId(2), 100 milliseconds)
          players_map.foreach(player => {
////////////////////////////////////////TODO prizes//////////////////////////////////////////////////////
            val player_rank = players_map.count(rank => rank._2 > player._2)

            MongoDBDriver.createAward(Award(last_award_id, player._1, "prize", received = false))
            last_award_id += 1

          })
        case None =>
      }
    }
  }

  def sendPlayerAward(session: ActorRef, player: Player): Unit = {
    MongoDBDriver.findAwardByPlayerId(player.id) onSuccess {
      case a => a.foreach(award => {
        session ! award.asJson
        MongoDBDriver.updateAward(Award(award.id, award.player_id, award.prize, received = true))
      })
    }
  }

  def addToFriends(task: StorageAddFriends): Unit = {
    MongoDBDriver.playerInfo(task.event.id) onSuccess {
      case pl => pl match {
        case Some(player) =>
          var new_friends_list: ArrayBuffer[Long] = ArrayBuffer.empty[Long]
          player.friends_list.foreach(friend => new_friends_list += friend)
          if (task.event.add)
            new_friends_list += task.event.friend_id
          else
            new_friends_list -= task.event.friend_id

          MongoDBDriver.updatePlayer(Player(player.id, player.name, player.password, player.rank, player.country, new_friends_list.toArray[Long]))

        case _ =>
          task.session ! ServerResp(ServerConnectionError.code).asJson
      }
    }
  }

  def addEventScore(task: StorageAddEventScore): Unit = {
    val rankings = Await.result(MongoDBDriver.findPlayerRankings(task.event.id, "event-" + task.event.event_id), 100 milliseconds)
    val new_rankings = Rankings("event-" + task.event.event_id, task.event.id, task.event.score)
    rankings.size match {
      case 1 =>
        MongoDBDriver.updateRankings(new_rankings)
      case 0 =>
        MongoDBDriver.createRankings(new_rankings)
    }
  }

}

object StorageService {

  case class StorageLogin(session: ActorRef, event: Login)

  case class StorageRegister(session: ActorRef, event: RegisterUser)

  case class StorageGameOver(players_ids: Map[Long, Int], winner_id: Long)

  case class StorageStats(session: ActorRef, event: StatsRequest)

  case class StorageAddFriends(session: ActorRef, event: AddToFriends)

  case class StorageAddEventScore(session: ActorRef, event: AddEventScore)

  case object StorageEvent

}