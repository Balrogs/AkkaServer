package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import aktor.gm.Room.{Abort, LostPlayer, PlayerInstance}
import aktor.storage.StorageService
import argonaut.Argonaut._
import global.game.Player
import global.server._

import scala.collection.mutable
import scala.concurrent.duration._


class Room(id: Long) extends Actor with ActorLogging {

  import context._

  private val period = 1 seconds
  private var players: Set[PlayerInstance] = Set.empty[PlayerInstance]
  private var isGameStarted = false
  private var actions: mutable.Map[TaskEvent, Boolean] = mutable.Map.empty[TaskEvent, Boolean]
  private var scheduler: Cancellable = _
  private var isPaused = true

  override def preStart() {

    log.info("Starting Room " + id)

    scheduler = context.system.scheduler.schedule(period, period, self, Tick)
  }

  override def receive = {

    case player: PlayerInstance => addPlayer(player)

    case task: TaskEvent => task.event.code match {

      case GameAction.code => applyAction(task)

      case GameOver.code => gameOver(task.event.asInstanceOf[GameOver])

    }
    case Abort => abortGame(-1)

    case Tick => tickAction()

    case task: LostPlayer => abortGame(task.player_id)

    case _ => log.info("unknown message")
  }

  def gameOver(task: GameOver): Unit = {

    val rank_diff = Math.abs(players.map(a => a.player.rank).reduce((a, b) => a - b))

    var winner_score: Int = if (rank_diff / 2 < 4) 6 else rank_diff / 2
    var looser_score: Int = if (rank_diff / 3 < 4) 4 else rank_diff / 3

    task.v_type match {
      case 0 =>
      case 1 =>
        winner_score /= 2
        looser_score /= 2
      case _ =>
        winner_score = 0
        looser_score = 0
    }

    players.foreach(p =>
      if (p.player.id == task.winner_id)
        p.session ! GameOver(task.winner_id, id, winner_score, AccessToken(-1, "server")).asJson
      else {
        if (p.player.rank - looser_score < 1)
          looser_score = p.player.rank - 1
        p.session ! GameOver(task.winner_id, id, looser_score, AccessToken(-1, "server")).asJson
      }
    )

    val storage = context.actorOf(Props[StorageService])
    storage ! StorageService.StorageGameOver(players.map(p =>
      if (p.player.id == task.winner_id) {
        (p.player.id, winner_score)
      } else
        (p.player.id, looser_score)
    ).toMap, task.winner_id)

    context stop self
  }

  def abortGame(player_id: Long): Unit = {
    if (player_id > 0) {
      val rank_diff = Math.abs(players.map(a => a.player.rank).reduce((a, b) => a - b))

      var winner_score: Int = if (rank_diff / 2 < 4) 6 else rank_diff / 2
      var looser_score: Int = if (rank_diff / 3 < 4) 4 else rank_diff / 3
      winner_score /= 2
      looser_score /= 2

      val winner_id = players.find(player => player.player.id != player_id).get.player.id

      players.foreach(p =>
        if (p.player.id == winner_id) {
          p.session ! GameOver(p.player.id, id, winner_score, AccessToken(-1, "server")).asJson
          sender() ! GameService.JoinSearch(p.session, p.player.id, Some(-1))
        }
      )

      val storage = context.actorOf(Props[StorageService])
      storage ! StorageService.StorageGameOver(players.map(p =>
        if (p.player.id == winner_id) {
          (p.player.id, winner_score)
        } else
          (p.player.id, looser_score)
      ).toMap, winner_id)
    } else
      players.foreach(p =>
        p.session ! ServerResp(GameAborted.code).asJson
      )
    context stop self
  }


  def addPlayer(player: PlayerInstance): Unit = {
    players = players.filter(p => p.player.id != player.player.id)
    if (players.size < 2) {
      players = players + player
      player.session ! ServerResp((-500 - players.size).toString).asJson
    }
    log.info("Players in room " + id + " : ")
    players.foreach(p => log.info(p.player.id.toString))

    if (players.size == 2 && !isGameStarted) {
      isGameStarted = true
      isPaused = false
      log.info("Game in room " + id + " STARTED!")
      players.foreach(p => p.session ! ServerResp(GameStarted.code).asJson)
    }

    if (players.size == 2 && isPaused) {
      isPaused = false
      log.info("Game in room " + id + " RESUMED!")
      player.session ! ServerResp(GameResumed.code).asJson
    }
  }

  def applyAction(task: TaskEvent): Unit = {
    log.info("Action: " + task.event.asInstanceOf[GameAction])
    actions.put(task, false)

  }

  def tickAction() = {
    if (!isPaused) {
      actions = actions.filter(a => !a._2).map(a => {
        log.info("Action sent: " + a._1.event.asInstanceOf[GameAction])
        players.find(p => p.session != a._1.session).get.session ! a._1.event.asInstanceOf[GameAction].asJson(GameAction.GameActionEncodeJson)
        (a._1, true)
      })
    }
  }

  def waitForPlayer(): Unit = {
    isPaused = true
    log.info("Game in room " + id + " PAUSED!")
    players.foreach(p => p.session ! ServerResp(GamePaused.code).asJson)
  }

  override def postStop() {
    scheduler.cancel()
    log.info("Stopping Room " + id)
  }

  case object Tick

}

object Room {

  def props(id: Long) = Props(new Room(id))

  case class PlayerInstance(session: ActorRef, player: Player)

  case object Abort

  case class LostPlayer(player_id: Long)

}
