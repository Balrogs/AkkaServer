package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import aktor.gm.Room.LostPlayer
import global._
import scala.collection.mutable
import argonaut._, Argonaut._

class GameService extends Actor with ActorLogging {

  import GameService._

  var players: Set[JoinLobby] = Set.empty[JoinLobby]

  val rooms: mutable.Map[Long, ActorRef] = mutable.Map.empty[Long, ActorRef]
  var idCounter = 1L

  case object IsEmpty


  override def preStart() {
    log.info("Starting game service")

  }

  override def receive = {

    case task: JoinLobby => addPlayer(task)

    case task: JoinGame => handleJoin(task)

    case task: TaskEvent => handleAction(task)

    case end: ActorRef => removePlayer(end)

    case _ => log.info("unknown message")
  }

  override def postStop() {
    log.info("Stopping game service")
  }

  def addPlayer(task: JoinLobby): Unit = {

    val player = players.find(p=>p.player.id == task.player.id)
    player match{
      case Some(pl) =>
        pl.session = task.session
      case None =>
        log.info("Adding player " + task.player.id + " " + task.player.name)
        players = players + task
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
    players.filter(p => p.player.id == task.event.player_id).head.room_id = Some(task.event.room_id)
    players.filter(p => p.player.id == task.event.player_id).head.room_id = Some(task.event.room_id)
    rooms.get(task.event.room_id).get ! Room.PlayerInstance(task.session, task.event.player_id, 0)
  }

  def handleAction(task: TaskEvent) = {
    rooms.get(task.event.asInstanceOf[GameAction].room_id).get ! TaskEvent(task.session, task.event)
  }

  def removePlayer(end: ActorRef) = {
    val p = players.find(p => p.session == end)
    p match {
      case Some(value) =>
        if (value.room_id.getOrElse(0) == 0){
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

  case class JoinLobby(var session: ActorRef, player: Player, var room_id:Option[Long])

}
