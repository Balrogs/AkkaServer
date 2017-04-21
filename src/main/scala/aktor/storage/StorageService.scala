package aktor.storage

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef}
import aktor.TaskService.TaskEvent
import aktor.gm.GameService.JoinLobby
import argonaut.Argonaut._
import global.game._
import global.server._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class StorageService extends Actor with ActorLogging {

  import aktor.storage.StorageService._

  val timeout = 1 seconds

  val gameService = context.actorSelection("/user/game")
  val taskService = context.actorSelection("/user/task")

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
      checkPlayer(task)
      context stop self

    case task: StorageAddEventScore =>
      addEventScore(task)
      context stop self

    case task: StorageGetGameEventEvent =>
      getEvent(task)
      context stop self

    case token: StorageAccessToken =>
      checkToken(token)
      context stop self

    case StorageEvent =>
      updateEvent()
      context stop self

    case _ =>
      log.info("unknown message " + self.path.name)
  }

  def authUser(task: StorageLogin): Unit = {

    val player = MongoDBDriver.AuthPlayer(task.event.name, task.event.password)
    player onSuccess {
      case p => p match {
        case Some(pl) =>
          MongoDBDriver.updatePlayer(Player(
            pl.id,
            pl.name,
            pl.password,
            pl.rank,
            pl.country,
            pl.friends_list,
            task.event.playerView
          ))
          val token = generateAccessToken()
          val accessToken = AccessToken(pl.id, token)
          MongoDBDriver.updateToken(accessToken)
          task.session ! ServerResp(AuthResp(accessToken.asJson.toString()).code).asJson
          gameService ! JoinLobby(task.session, pl, None, None)
        case None =>
          task.session ! ServerResp(LoginError.code).asJson
      }
    }
    player onFailure {
      case _ => task.session ! ServerResp(LoginError.code).asJson
    }

  }

  def sendPlayerAward(session: ActorRef, id: Long): Unit = {
    MongoDBDriver.findAwardByPlayerId(id) onSuccess {
      case a => a.foreach(award => {
        session ! award.asJson
        MongoDBDriver.updateAward(Award(award.id, award.player_id, award.reward, received = true))
      })
    }
  }

  def registerUser(task: StorageRegister): Unit = {

    val reg = MongoDBDriver.playerInfoByName(task.event.name)
    reg onSuccess {
      case Some(player) =>
          task.session ! ServerResp(RegisterErrorNameExists.code).asJson
        case None =>
          val idRequest = MongoDBDriver.getLastId(1)
          idRequest onSuccess {
            case id =>
              val pl = Player(
                id,
                task.event.name,
                task.event.password,
                1,
                task.event.country,
                Array.empty[String],
                CustomPlayerView(0, 0, 0)
              )
              MongoDBDriver.createPlayer(pl)

              val accessToken = AccessToken(id, generateAccessToken())
              MongoDBDriver.createToken(accessToken)
              task.session ! ServerResp(AuthResp(accessToken.asJson.toString()).code).asJson
              gameService ! JoinLobby(task.session, pl, None, None)
          }
          idRequest onFailure {
            case _ => task.session ! ServerResp(ServerConnectionError.code).asJson
          }
    }
    reg onFailure {
      case _ => task.session ! ServerResp(ServerConnectionError.code).asJson
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
                MongoDBDriver.updatePlayer(Player(pl.id, pl.name, pl.password, pl.rank + p._2, pl.country, pl.friends_list, pl.playerView))

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
        userInfo(task, isPrivate = false)

    }
  }

  def userInfo(task: StorageStats, isPrivate: Boolean): Unit = {
    MongoDBDriver.playerInfoByName(task.event.name) onSuccess {
      case pl => pl match {
        case Some(player) =>
          MongoDBDriver.findStatsById(player.id) onSuccess {
            case st => st match {
              case Some(stats) =>
                val battles_win = stats.battles.count(b => {
                  b.winner_id == stats.id
                })

                val global_rankings = Await.result(MongoDBDriver.findRankingsByName("global"), timeout).map(e => (e.player_id, e.rank))

                val player_global_rank = global_rankings.find(p => p._1 == player.id).get._2

                val global_rank = global_rankings.count(rank => rank._2 > player_global_rank)

                val country_rankings = Await.result(MongoDBDriver.findRankingsByName("country-" + player.country), timeout).map(e => (e.player_id, e.rank))

                val player_country_rank = country_rankings.find(p => p._1 == player.id).get._2

                val country_rank = country_rankings.count(rank => rank._2 > player_country_rank)

                gameService ! (UserInfo(player.name, player.country, player.playerView, player.friends_list.map((_, true)), player.rank, global_rank + 1, country_rank + 1, stats.battles.size, battles_win, stats.battles.size - battles_win, stats.date_reg), isPrivate)

              case None => task.session ! ServerResp(ServerConnectionError.code).asJson
            }
          }
        case None =>
          task.session ! ServerResp(UserNotFound.code).asJson
      }
    }
  }

  def updateEvent(): Unit = {
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

  def getEvent(task : StorageGetGameEventEvent): Unit = {
    MongoDBDriver.findGameEvents() onSuccess {
      case list => list.find(event => {
        val localTime = LocalDateTime.now()
        if (LocalDateTime.parse(event.date_begin).isBefore(localTime) && LocalDateTime.parse(event.date_end).isAfter(localTime))
          true
        else
          false
      }) match {
        case Some(event) =>
          task.session ! event.asJson
        case None =>
      }
    }
    sendPlayerAward(task.session, task.event.player_id)
  }


  def determineAwards(event_id: Long): Unit = {
    MongoDBDriver.findEventById(event_id) onSuccess {
      case e => e match {
        case Some(event) =>
          val players_map = Await.result(MongoDBDriver.findRankingsByName("event-" + event_id), timeout).map(rankings => (rankings.player_id, rankings.rank))
          var last_award_id = Await.result(MongoDBDriver.getLastId(2), timeout)
          players_map.foreach(player => {
            val player_rank = players_map.count(rank => rank._2 > player._2)
            if(player_rank <= 10){
              MongoDBDriver.createAward(Award(last_award_id, player._1, event.description.rewards(player_rank), received = false))
            } else if(player_rank <= 100){
              MongoDBDriver.createAward(Award(last_award_id, player._1, event.description.rewards(11), received = false))
            } else {
              MongoDBDriver.createAward(Award(last_award_id, player._1, event.description.rewards(12), received = false))
            }
            last_award_id += 1

          })
          MongoDBDriver.updateGameEvent(GameEvent(event.id, event.date_begin, event.date_end, event.description, isAwardsSet = true))
        case None =>
      }
    }
  }

  def checkPlayer(task: StorageAddFriends): Unit = {
    MongoDBDriver.playerInfoByName(task.event.friend_name) onSuccess {
      case pl => pl match {
        case Some(player) =>
          addToFriends(task)
        case _ =>
          task.session ! ServerResp(FriendNotFound.code).asJson
      }
    }
  }

  def addToFriends(task: StorageAddFriends): Unit = {
    MongoDBDriver.playerInfo(task.event.id) onSuccess {
      case pl => pl match {
        case Some(player) =>
          var new_friends_list = player.friends_list.toBuffer

          if (new_friends_list.contains(task.event.friend_name)) {
            new_friends_list = new_friends_list.filter(_.equalsIgnoreCase(task.event.friend_name))
            task.session ! ServerResp(FriendRemoved.code).asJson
          }
          else {
            new_friends_list.append(task.event.friend_name)
            task.session ! ServerResp(FriendAdded.code).asJson
          }

          MongoDBDriver.updatePlayer(Player(player.id, player.name, player.password, player.rank, player.country, new_friends_list.toArray, player.playerView))
          userInfo(StorageStats(task.session, UserInfoRequest(3, player.name)), isPrivate = true)
        case _ =>
          task.session ! ServerResp(FriendNotFound.code).asJson
      }
    }
  }

  def addEventScore(task: StorageAddEventScore): Unit = {
    val event_name = "event-" + task.event.event_id
    val rankings = Await.result(MongoDBDriver.findPlayerRankings(task.event.id, event_name), timeout)
    val new_rankings = Rankings(event_name, task.event.id, task.event.score)
    Await.result(rankings.size match {
      case 1 =>
        MongoDBDriver.updateRankings(new_rankings)
      case 0 =>
        MongoDBDriver.createRankings(new_rankings)
    }, timeout)
    task.session ! returnStats(StorageStats(task.session, UserInfoRequest(2, event_name)))
  }

  def checkToken(token: StorageAccessToken): Unit = {
    MongoDBDriver.findTokenById(token.token.id) onSuccess {
      case tok => tok match {
        case Some(t) =>
          if (t.token == token.token.token) {
            taskService ! token.event
          }
        case _ =>
      }
    }
  }

  def generateAccessToken(): String = {
    val token = Random.nextString(20)
    val isExist = Await.result(MongoDBDriver.findToken(token), timeout)
    isExist match {
      case Some(t) => generateAccessToken()
      case None => token
    }
  }

  override def postStop() {

    log.info("Stopping storage service")

  }

}

object StorageService {

  case class StorageLogin(session: ActorRef, event: Login)

  case class StorageRegister(session: ActorRef, event: RegisterUser)

  case class StorageGameOver(players_ids: Map[Long, Int], winner_id: Long)

  case class StorageStats(session: ActorRef, event: UserInfoRequest)

  case class StorageAddFriends(session: ActorRef, event: AddToFriends)

  case class StorageAddEventScore(session: ActorRef, event: AddEventScore)

  case class StorageGetGameEventEvent(session: ActorRef, event: GetGameEvent)

  case class StorageAccessToken(event: TaskEvent, token: AccessToken)

  case object StorageEvent

}