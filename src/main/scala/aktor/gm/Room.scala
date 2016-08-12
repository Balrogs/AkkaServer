package aktor.gm

import akka.actor._
import aktor.TaskService.TaskEvent
import aktor.gm.Room.JoinRoom
import global._
import argonaut._, Argonaut._


class Room(id:Long) extends Actor with ActorLogging {

  var players: Set[JoinRoom] = Set.empty[JoinRoom]

  case object Tick
  case object RoomStop

  override def preStart() {

    log.info("Starting Room " + id)

  }

  override def receive = {

    case player:JoinRoom => addPlayer(player)

    case task:TaskEvent => applyAction(task)

    case RoomStop => context stop self

    case _ => log.info("unknown message")
  }


  override def postStop() {

    log.info("Stopping Room " + id)

  }

  def addPlayer(player:JoinRoom): Unit ={
    if(players.size < 2){
      players = players + player
    }
    log.info("Players in room " + id + " : ")
    players.foreach(p=>log.info(p.player_id.toString))
  }

  def applyAction(task:TaskEvent): Unit ={
    val event = task.event.asInstanceOf[GameAction]
    log.info("Action: " + event)
    players.filter(p=> p.player_id != event.player_id).head.session !  task.event.asInstanceOf[GameAction].asJson
  }

}

object Room {

  case class JoinRoom(session: ActorRef, player_id: Long)

  def props(id: Long) = Props(new Room(id))

}
