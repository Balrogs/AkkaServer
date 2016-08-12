package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import global.{EnterRoom, InviteIntoRoom, GameAction, Player}
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

    case _ => log.info("unknown message")
  }

  override def postStop() {
    log.info("Stopping game service")
  }

  def addPlayer(task: JoinLobby): Unit = {
    if (!players.exists(p=>p.player.id==task.player.id)) {
      log.info("Adding player " + task.player.id + " " + task.player.name)
      players = players + task
    } else {
      log.info("Changing player isInGame state to false " + task.player.id + " " + task.player.name)
      players.filter(p=>p.player.id == task.player.id).head.player.isInGame = false
    }

    log.info("Players: ")
    players.foreach(p=>log.info(p.player.id + " " + p.player.name))
    managePlayers()

  }

  def managePlayers():Unit = {
    players.foreach(p => {
      players.foreach(p2 => {
        if (p != p2 && !p.player.isInGame && !p2.player.isInGame && p2.player.rank + 100 > p.player.rank && p2.player.rank - 100 < p.player.rank) {
          val room_id = createRoom()
          p.session ! InviteIntoRoom(p2.player.id, p2.player.rank,"token 1", room_id).asJson
          p2.session ! InviteIntoRoom(p.player.id, p.player.rank,"token 2", room_id).asJson
          p.player.isInGame = true
          p2.player.isInGame = true
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
    idCounter-1
  }

  def handleJoin(task: JoinGame) = {
    rooms.get(task.event.room_id).get ! Room.JoinRoom(task.session, task.event.player_id)
  }

  def handleAction(task: TaskEvent) = {
    rooms.get(task.event.asInstanceOf[GameAction].room_id).get ! TaskEvent(task.session, task.event)
  }

}

object GameService {

  case class JoinGame(session: ActorRef, event:EnterRoom)

  case class JoinLobby(session: ActorRef, player: Player)

}
