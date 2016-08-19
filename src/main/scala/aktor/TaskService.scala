package aktor

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import aktor.gm.GameService
import aktor.storage.StorageService
import global._
import global.server._

class TaskService extends Actor with ActorLogging {

  import TaskService._

  val gameService = context.actorSelection("/user/game")


  override def preStart() {
    log.info("Starting task service")
  }

  override def receive = {
    case data : TaskEvent => handlePacket(data)

    case end : ActorRef => gameService ! end

    case _ => log.info("unknown message")
  }

  override def postStop() {
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
        log.info("User " + task.event.asInstanceOf[RegisterUser].name + " tries to register")
        val storage = context.actorOf(Props[StorageService])
        storage ! StorageService.StorageRegister(task.session,task.event.asInstanceOf[RegisterUser])

      case GameAction.code =>
        log.info("User tries to apply action")
        gameService ! task

      case GameOver.code =>
        gameService ! task

      case EnterRoom.code =>
        log.info("User tries to enter the room")
        gameService ! GameService.JoinGame(task.session, task.event.asInstanceOf[EnterRoom])

      case StatsRequest.code =>

        log.info("User tries to get stats")
        val storage = context.actorOf(Props[StorageService])
        storage ! StorageService.StorageStats(task.session, task.event.asInstanceOf[StatsRequest])

      case AddToFriends.code =>
        log.info("User tries to add friend")
        val storage = context.actorOf(Props[StorageService])
        storage ! StorageService.StorageAddFriends(task.session, task.event.asInstanceOf[AddToFriends])


      case AddEventScore.code =>
        log.info("User tries to add event score")
        val storage = context.actorOf(Props[StorageService])
        storage ! StorageService.StorageAddEventScore(task.session, task.event.asInstanceOf[AddEventScore])


      case _ =>
        log.info("Unknown message")
    }
  }
}

object TaskService {

  case class TaskEvent(session: ActorRef, event: EventMsg)

}