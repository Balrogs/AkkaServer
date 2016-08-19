package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import aktor.gm.Room.LostPlayer
import aktor.storage.StorageService
import global._
import global.game.{Player, GameEvent}
import global.server.{GameOver, GameAction, InviteIntoRoom, EnterRoom}
import scala.collection.mutable
import argonaut._, Argonaut._
import scala.concurrent.duration._

class GameService extends Actor with ActorLogging {

  import GameService._
  import context._

  var players: Set[JoinLobby] = Set.empty[JoinLobby]

  val rooms: mutable.Map[Long, ActorRef] = mutable.Map.empty[Long, ActorRef]
  var idCounter = 1L

  var current_GameEvent: GameEvent = null

  case object IsEmpty

  private var scheduler: Cancellable = _

  private val period = 1 day

  override def preStart() {
    log.info("Starting game service")

    scheduler = context.system.scheduler.schedule(period, period, self, UpdateGameEvent)
  }

  override def receive = {

    case task: JoinLobby => addPlayer(task)

    case task: JoinGame => handleJoin(task)

    case task: TaskEvent => handleAction(task)

    case end: ActorRef => removePlayer(end)

    case event: GameEvent => current_GameEvent = event

    case UpdateGameEvent => updateGameEvent()

    case _ => log.info("unknown message")
  }

  override def postStop() {
    scheduler.cancel()
    log.info("Stopping game service")
  }

  def updateGameEvent() = {
    val storage = context.actorOf(Props[StorageService])
    storage ! StorageService.StorageEvent
  }

  def addPlayer(task: JoinLobby): Unit = {

    val player = players.find(p => p.player.id == task.player.id)

    player match {
      case Some(pl) =>
        pl.session = task.session
      case None =>
        log.info("Adding player " + task.player.id + " " + task.player.name)
        players = players + task
        if (current_GameEvent != null) {
          task.session ! current_GameEvent.asJson
        }
    }
    showPlayers()
  }

  def showPlayers() = {
    log.info("Players: ")
    players.foreach(p => log.info(p.player.id + " " + p.player.name))
    managePlayers()
  }

  def managePlayers(): Unit = {
    players.foreach(p => {
      players.foreach(p2 => {
        if (p != p2 && p.room_id.getOrElse(0) == 0 && p2.room_id.getOrElse(0) == 0 && p2.player.rank + 100 > p.player.rank && p2.player.rank - 100 < p.player.rank) {
          val room_id = createRoom()
          p.session ! InviteIntoRoom(p2.player.name, p2.player.rank, room_id).asJson
          p2.session ! InviteIntoRoom(p.player.name, p.player.rank, room_id).asJson
          return
        }
      })
    })
  }

  def sendAsk(first_player: JoinLobby, second_player: JoinLobby): Unit = {
    if (rooms.isEmpty) {
      createRoom()
    }
  }

  def createRoom(): Long = {
    val room = context.actorOf(Room.props(idCounter), "room-" + idCounter)
    rooms.put(idCounter, room)
    idCounter += 1
    idCounter - 1
  }

  def handleJoin(task: JoinGame) = {

    val player = players.find(p => p.player.id == task.event.player_id).get
    player.room_id = Some(task.event.room_id)

    rooms.get(task.event.room_id) match {
      case Some(room) => room ! Room.PlayerInstance(task.session, player.player)
      case None =>
    }
  }

  def handleAction(task: TaskEvent) = {
    task.event.code match {
      case GameAction.code =>
        rooms.get(task.event.asInstanceOf[GameAction].room_id) match {
          case Some(room) => room ! TaskEvent(task.session, task.event)
          case None =>
        }
      case GameOver.code =>
        rooms.get(task.event.asInstanceOf[GameOver].room_id) match {
          case Some(room) => room ! TaskEvent(task.session, task.event)
          case None =>
        }
      case _ =>
    }

  }

  def removePlayer(end: ActorRef) = {
    val p = players.find(p => p.session == end)
    p match {
      case Some(value) =>
        if (value.room_id.getOrElse(0) == 0) {
          players = players.filter(p => p.session != end)
          showPlayers()
        } else {
          rooms.get(value.room_id.get).get ! LostPlayer
        }
      case None =>
    }
  }
}

object GameService {

  case class JoinGame(session: ActorRef, event: EnterRoom)

  case class JoinLobby(var session: ActorRef, player: Player, var room_id: Option[Long])

  case object UpdateGameEvent

}
