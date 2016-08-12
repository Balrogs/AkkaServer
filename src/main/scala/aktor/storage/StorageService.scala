package aktor.storage

import akka.actor.{ActorRef, Actor, ActorLogging}
import aktor.gm.GameService.JoinLobby
import global.{AuthResp, Player, RegisterUser, Login}
import argonaut._, Argonaut._

import scala.util.Random

class StorageService extends Actor with ActorLogging {

  import StorageService._

  val gameService = context.actorSelection("/user/game")

  override def preStart() {
    log.info("Starting storage service")
  }

  override def receive = {
    case task:StorageLogin =>
      log.info("Login - OK")
      gameService ! JoinLobby(task.session, Player(task.event.id,"some name from db", "generated token", Random.nextInt(100),false))
      context stop self
    case task:StorageRegister =>
      log.info("Register - OK")
      task.session ! AuthResp(true,"generated token").asJson
      gameService ! JoinLobby(task.session, Player(task.event.id,"some name from db", "generated token", Random.nextInt(100),false))
      context stop self
    case _ => log.info("unknown message " + self.path.name)
  }

  override def postStop() {
    log.info("Stopping storage service")
  }
}

object StorageService {

  case class StorageLogin(session:ActorRef, event:Login)
  case class StorageRegister(session:ActorRef, event:RegisterUser)

}