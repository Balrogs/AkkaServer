package aktor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.{Timeout, ByteString}
import aktor.storage.StorageService
import argonaut.Argonaut._
import argonaut._
import global.server._
import scala.concurrent.duration._


class Session(val connection: ActorRef) extends Actor with ActorLogging {

  import akka.io.Tcp._

  val taskService = context.actorSelection("/user/task")

  implicit val timeout = Timeout(5 seconds)

  override def preStart() {

    log.info("Session start: {}", toString)

  }

  def receive = {

    case Received(data) => Parse(data)

    case e: Json =>
      println(e.toString())
      connection ! Write(ByteString(e.toString()))

    case _: Tcp.ConnectionClosed ⇒
      taskService ! self
      context stop self

  }

  def Parse(string: ByteString): Unit = {
    var message = string.utf8String.trim
    var id = message.indexOf("}{")
    while (id > 0) {
      parseSingleMessage(message.substring(0, id + 1))
      message = message.substring(id + 1)
      id = message.indexOf("}{")
    }
    parseSingleMessage(message)
  }

  def parseSingleMessage(message: String): Unit = {

    log.info("Received: " + message)

    val event_type = message.decodeOption[EventType]

    val event_token = message.decodeOption[EventToken]

    event_token match {
      case Some(token) =>
        sendToStorageService(message, event_type, token.token)
      case _ =>
        sendToTaskService(message, event_type)
    }
  }

  override def postStop() {
    log.info("Session stop: {}", toString)
  }

  def sendToTaskService(message: String, event_type: Option[EventType]): Unit = {
    event_type match {
      case Some(EventType(1)) =>
        taskService ! TaskService.TaskEvent(self, message.decodeOption[Login].get)
      case Some(EventType(2)) =>
        taskService ! TaskService.TaskEvent(self, message.decodeOption[RegisterUser].get)
      case Some(EventType(9)) =>
        taskService ! TaskService.TaskEvent(self, message.decodeOption[UserInfoRequest].get)
      case Some(_) =>
      case None =>
        if (message.contains("GET")) {
          log.info("GET message: {}", message)
          connection ! Write(ByteString("This is Archers Unlimited GameServer! Hello, Billy!"))
        } else {
          log.info("Unknown message: {}", message)
        }
    }
  }

  def sendToStorageService(message: String, event_type: Option[EventType], token: AccessToken): Unit = {
    val storage = context.actorOf(Props[StorageService])
    event_type match {
      case Some(EventType(4)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[EnterRoom].get), token)
      case Some(EventType(40)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[EnterLobby].get), token)
      case Some(EventType(41)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[DenyInvite].get), token)
      case Some(EventType(6)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[GameAction].get), token)
      case Some(EventType(5)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[InviteIntoRoom].get), token)
      case Some(EventType(7)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[GameOver].get), token)
      case Some(EventType(11)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[AddToFriends].get), token)
      case Some(EventType(12)) =>
        storage ! StorageService.StorageAccessToken(TaskService.TaskEvent(self, message.decodeOption[AddEventScore].get), token)
      case Some(_) =>
        log.info("Unknown message: {}", message)
      case None =>
        log.info("Unknown message: {}", message)
    }
  }

}

object Session {

  def props(connect: ActorRef) = Props(
    new Session(connect)
  )

}
