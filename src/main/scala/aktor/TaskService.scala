package aktor

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import aktor.gm.GameService
import aktor.storage.StorageService
import global._

class TaskService extends Actor with ActorLogging {

  import TaskService._

  val gameService = context.actorSelection("/user/game")


  override def preStart() {
    log.info("Starting task service")
  }

  override def receive = {
    case data : TaskEvent => handlePacket(data)

    case _ => log.info("unknown message")
  }

  override def postStop() {
    // clean up resources
    log.info("Stoping task service")
  }

  // ----- handles -----
  def handlePacket(task: TaskEvent) = {
    task.event.code match {
      case Login.code =>
        log.info("User " + task.event.asInstanceOf[Login].id + " tries to login")
        val storage = context.actorOf(Props[StorageService])
        storage ! StorageService.StorageLogin(task.session,task.event.asInstanceOf[Login])
      case RegisterUser.code =>
        log.info("User " + task.event.asInstanceOf[RegisterUser].id + " tries to register")
        val storage = context.actorOf(Props[StorageService])
        storage ! StorageService.StorageRegister(task.session,task.event.asInstanceOf[RegisterUser])
      case GameAction.code =>
        log.info("User tries to apply action")
        gameService ! task
      case EnterRoom.code =>
        log.info("User tries to enter the room")
        gameService ! GameService.JoinGame(task.session, task.event.asInstanceOf[EnterRoom])
      case _ =>
        log.info("Unknown message")
    }
  }
}

object TaskService {

  case class TaskEvent(session: ActorRef, event: EventMsg)

}