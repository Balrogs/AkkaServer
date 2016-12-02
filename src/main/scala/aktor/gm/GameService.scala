package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import aktor.gm.Room.LostPlayer
import aktor.storage.StorageService
import argonaut.Argonaut._
import global.game.{GameEvent, Player}
import global.server._

import scala.collection.mutable
import scala.concurrent.duration._

class GameService extends Actor with ActorLogging {

  import aktor.gm.GameService._
  import context._

  var rooms: mutable.Map[Long, ActorRef] = mutable.Map.empty[Long, ActorRef]
  private val period = 1 day
  var players: Set[JoinLobby] = Set.empty[JoinLobby]
  var idCounter = 1L
  var current_GameEvent: GameEvent = null
  private var scheduler: Cancellable = _

  override def preStart() {
    log.info("Starting game service")

    scheduler = context.system.scheduler.schedule(period, period, self, UpdateGameEvent)
    self ! UpdateGameEvent
  }

  override def receive = {

    case task: JoinLobby => addPlayer(task)

    case task: JoinGame => handleJoin(task)

    case task: InvitePlayer => invitePlayer(task)

    case task: JoinSearch => handleJoinSearch(task)

    case task: DenyGame => removeRoom(task)

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

    log.info(player.toString)

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

  def handleJoinSearch(task: JoinSearch): Unit = {

    players.foreach(player => {
      if (player.player.id == task.player_id) {
        player.room_id = task.room
      }
    })

    showPlayers()
  }

  def invitePlayer(task: InvitePlayer): Unit = {

  }

  def showPlayers() = {
    log.info("Players: ")
    players.foreach(p => log.info("id: " + p.player.id + " Name: " + p.player.name))
    managePlayers()
  }

  def managePlayers(): Unit = {
    players.foreach(p => {
      players.foreach(p2 => {
        if (p != p2 && p.room_id.getOrElse(0) == 0 && p2.room_id.getOrElse(0) == 0 && p2.player.rank + 100 > p.player.rank && p2.player.rank - 100 < p.player.rank) {
          val room_id = createRoom()
          p.session ! InviteIntoRoom(p2.player.id, p2.player.name, p2.player.rank, room_id, AccessToken(-1, "server")).asJson
          p2.session ! InviteIntoRoom(p.player.id, p.player.name, p.player.rank, room_id, AccessToken(-1, "server")).asJson
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

    rooms.get(task.event.room_id) match {
      case Some(room) =>
        player.room_id = Some(task.event.room_id)
        room ! Room.PlayerInstance(task.session, player.player)
      case None => player.session ! ServerResp(GameAborted.code).asJson
    }
  }

  def removeRoom(task: DenyGame) = {

    val player = players.find(p => p.player.id == task.event.player_id).get
    player.room_id = Some(task.event.room_id)

    rooms.get(task.event.room_id) match {
      case Some(room) =>
        rooms.remove(task.event.room_id)
        player.room_id = None
        room ! Room.Abort
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
        players = players.filter(pl => pl.session != end)
        if (value.room_id.getOrElse(0) != 0) {
          value.room_id match {
            case Some(id) =>
              rooms.get(id) match {
                case Some(room) => room ! LostPlayer(value.player.id)
              }
          }
        }
        showPlayers()
      case None =>
    }
  }

  case object IsEmpty

}

object GameService {

  case class DenyGame(session: ActorRef, event: DenyInvite)

  case class JoinGame(session: ActorRef, event: EnterRoom)

  case class InvitePlayer(session: ActorRef, event: InviteIntoRoom)

  case class JoinSearch(var session: ActorRef, player_id: Long, room: Option[Long])

  case class JoinLobby(var session: ActorRef, player: Player, var room_id: Option[Long])

  case object UpdateGameEvent

}
