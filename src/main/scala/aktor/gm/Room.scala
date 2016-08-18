package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import aktor.gm.Room.{LostPlayer, PlayerInstance}
import aktor.storage.StorageService
import global._
import argonaut._, Argonaut._

import scala.collection.mutable
import scala.concurrent.duration._


class Room(id: Long) extends Actor with ActorLogging {

  import context._

  private var players: Set[PlayerInstance] = Set.empty[PlayerInstance]

  private val MAX_ACTIONS_COUNT = 10

  private var isGameStarted = false

  private var actions: mutable.Map[TaskEvent, Boolean] = mutable.Map.empty[TaskEvent, Boolean]

  private var scheduler: Cancellable = _

  private val period = 1 seconds

  private var isPaused = true

  case object RoomStop

  case object Tick


  override def preStart() {

    log.info("Starting Room " + id)

    scheduler = context.system.scheduler.schedule(period, period, self, Tick)
  }

  override def receive = {

    case player: PlayerInstance => addPlayer(player)

    case task: TaskEvent => applyAction(task)

    case Tick => tickAction

    case LostPlayer => waitForPlayer()

    case RoomStop =>
      gameOver()
      context stop self

    case _ => log.info("unknown message")
  }


  override def postStop() {
    scheduler.cancel()
    log.info("Stopping Room " + id)
  }

  def gameOver(): Unit = {

    val winner_id: Long = players.reduceLeft((x, y) => if (x.score > y.score) x else y).id
    players.foreach(p => p.session ! GameOver(winner_id, id).asJson)

    val storage = context.actorOf(Props[StorageService])
    storage ! StorageService.StorageGameOver(players.map(p => p.id).toArray[Long], winner_id)
  }

  def addPlayer(player: PlayerInstance): Unit = {
    players = players.filter(p => p.id != player.id)
    if (players.size < 2) {
      players = players + player
      player.session ! ServerResp(UserEnteredRoom.code).asJson
    }
    log.info("Players in room " + id + " : ")
    players.foreach(p => log.info(p.id.toString))

    if (players.size == 2 && !isGameStarted) {
      isGameStarted = true
      isPaused = false
      log.info("Game in room " + id + " STARTED!")
      players.foreach(p=> p.session ! ServerResp(GameStarted.code).asJson)
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
    if(!isPaused) {
      actions = actions.filter(a => !a._2).map(a => {
        log.info("Action sent: " + a._1.event.asInstanceOf[GameAction])
        players.find(p => p.session != a._1.session).get.session ! a._1.event.asInstanceOf[GameAction].asJson
        (a._1, true)
      })
      if (actions.count(a => a._2) >= MAX_ACTIONS_COUNT) {
        self ! RoomStop
      }
    }
  }

  def waitForPlayer(): Unit ={
    isPaused = true
    log.info("Game in room " + id + " PAUSED!")
    players.foreach(p=> p.session ! ServerResp(GamePaused.code).asJson)
  }

}

object Room {

  case class PlayerInstance(session: ActorRef, id: Long, score: Int)
  case object LostPlayer
  def props(id: Long) = Props(new Room(id))

}
