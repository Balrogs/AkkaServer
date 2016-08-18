package aktor.storage

import akka.actor.{ActorRef, Actor, ActorLogging}
import aktor.gm.GameService.JoinLobby
import global._
import argonaut._, Argonaut._

import scala.collection.mutable.ArrayBuffer
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

    case task:StorageStats =>
      returnStats(task)
      context stop self

    case _ =>
      log.info("unknown message " + self.path.name)
  }

  override def postStop() {
    log.info("Stopping storage service")
  }

  def authUser(task:StorageLogin): Unit ={
    val player = MongoDBDriver.AuthPlayer(task.event.id, task.event.password)
    player onSuccess {
      case p => p match {
        case Some(pl) =>
          task.session ! ServerResp(AuthResp(pl.id).code).asJson
          gameService ! JoinLobby(task.session, pl, None)
        case None =>
          task.session ! ServerResp(LoginError.code).asJson
      }
    }
    player onFailure {
      case _ => task.session ! ServerResp(ServerConnectionError.code).asJson
    }
  }
  def registerUser(task:StorageRegister): Unit ={
    MongoDBDriver.playerInfoByName(task.event.name) onSuccess {
      case pl => pl match{
        case Some(pl) =>
          task.session ! ServerResp(RegisterErrorNameExists.code).asJson
        case None =>
          MongoDBDriver.getLastId(1) onSuccess {
            case id =>
              MongoDBDriver.createPlayer(Player(
                id,
                task.event.name,
                task.event.password,
                1,
                Array.empty[Int]
              )) onSuccess {
                case _ => task.session ! ServerResp(AuthResp(id).code).asJson
              }
          }
      }
    }
  }
  def finalizeGame(task:StorageGameOver): Unit ={
    MongoDBDriver.getLastId(3) onSuccess {
      case id =>
        MongoDBDriver.createBattle(Battle(id.toLong, task.players_ids, task.winner_id))
        task.players_ids.foreach(p =>{
          MongoDBDriver.findStatsById(p) onSuccess {
            case stats => stats match {
              case Some(s) =>
                val battles = ArrayBuffer[Long](id.toLong)
                s.battles_ids.foreach(b=> battles += b)
                log.info(battles.toArray.toString)
                MongoDBDriver.updateStats(Stats(p, battles.toArray, s.date_reg))
              case None =>
            }
          }
        })
    }

  }
  def returnStats(task:StorageStats): Unit =  {
    MongoDBDriver.playerInfoByName(task.event.name) onSuccess {
      case pl => pl match{
        case Some(pl) =>
          log.info("Stats player name: " + pl.name)
          MongoDBDriver.findStatsById(pl.id) onSuccess {
            case st => st match {
              case Some(stats) =>
                log.info("Stats id: " + stats.id)
                var battles_win = 0
                stats.battles_ids.foreach(b=>
                  MongoDBDriver.findBattleById(b) onSuccess{
                    case ans => ans match{
                      case Some(battle) =>
                        log.info("Battle id: " + battle.id)
                        if(battle.winner_id == pl.id){
                          battles_win+=1
                        }
                      case None =>
                    }
                  }
                )
                task.session ! PlayersStats(pl.name, pl.rank, stats.battles_ids.size, battles_win, stats.battles_ids.size-battles_win, stats.date_reg).asJson
              case None => task.session ! ServerResp(ServerConnectionError.code).asJson
            }
          }
        case None =>
          task.session ! ServerResp(UserNotFound.code).asJson
      }
    }
  }
}

object StorageService {

  case class StorageLogin(session: ActorRef, event: Login)

  case class StorageRegister(session: ActorRef, event: RegisterUser)

  case class StorageGameOver(players_ids: Array[Long], winner_id: Long)

  case class StorageStats(session: ActorRef, event: StatsRequest)

}