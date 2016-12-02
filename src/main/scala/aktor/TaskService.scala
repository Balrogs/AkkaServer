package aktor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import aktor.gm.GameService
import aktor.gm.GameService.JoinLobby
import aktor.storage.StorageService
import global.server._

class TaskService extends Actor with ActorLogging {

  import aktor.TaskService._

  val gameService = context.actorSelection("/user/game")


  override def preStart() {
    log.info("Starting task service")
  }

  override def receive = {
    case data: TaskEvent => handlePacket(data)

    case end: ActorRef => gameService ! end

    case _ => log.info("unknown message")
  }

  // ----- handles -----
  def handlePacket(task: TaskEvent) = {

    val storage = context.actorOf(Props[StorageService])

    task.event.code match {
      case Login.code =>
        log.info("User " + task.event.asInstanceOf[Login].name + " tries to login")
        storage ! StorageService.StorageLogin(task.session, task.event.asInstanceOf[Login])

      case RegisterUser.code =>
        log.info("User " + task.event.asInstanceOf[RegisterUser].name + " tries to register")
        storage ! StorageService.StorageRegister(task.session, task.event.asInstanceOf[RegisterUser])

      case GameAction.code =>
        log.info("User tries to apply action")

        gameService ! task

      case GameOver.code =>
        gameService ! task

      case EnterLobby.code =>
        log.info("User tries to enter the lobby")
        gameService ! GameService.JoinSearch(task.session, task.event.asInstanceOf[EnterLobby].player_id, None)

      case EnterRoom.code =>
        log.info("User tries to enter the room")
        gameService ! GameService.JoinGame(task.session, task.event.asInstanceOf[EnterRoom])

      case InviteIntoRoom.code =>
        log.info("User tries to invite other player to game")
        gameService ! GameService.InvitePlayer(task.session, task.event.asInstanceOf[InviteIntoRoom])

      case DenyInvite.code =>
        gameService ! GameService.DenyGame(task.session, task.event.asInstanceOf[DenyInvite])

      case UserInfoRequest.code =>

        log.info("User tries to get info")
        storage ! StorageService.StorageStats(task.session, task.event.asInstanceOf[UserInfoRequest])

      case AddToFriends.code =>
        log.info("User tries to add friend")
        storage ! StorageService.StorageAddFriends(task.session, task.event.asInstanceOf[AddToFriends])


      case AddEventScore.code =>
        log.info("User tries to add event score")
        storage ! StorageService.StorageAddEventScore(task.session, task.event.asInstanceOf[AddEventScore])


      case _ =>
        log.info("Unknown message")
    }
  }

  override def postStop() {
    log.info("Stoping task service")
  }
}

object TaskService {

  case class TaskEvent(session: ActorRef, event: EventMsg)

}