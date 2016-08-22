package aktor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import argonaut.Argonaut._
import argonaut._
import global.server._

class Session(val connection: ActorRef) extends Actor with ActorLogging {

  import akka.io.Tcp._

  val taskService = context.actorSelection("/user/task")


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
    val message = string.utf8String.trim

    log.info("Received: " + message)

    val event_type = message.decodeOption[EventType]
    event_type match {
      case Some(EventType(1)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[Login].get)
      case Some(EventType(2)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[RegisterUser].get)
      case Some(EventType(4)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[EnterRoom].get)
      case Some(EventType(6)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[GameAction].get)
      case Some(EventType(5)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[InviteIntoRoom].get)
      case Some(EventType(7)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[GameOver].get)
      case Some(EventType(9)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[UserInfoRequest].get)
      case Some(EventType(11)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[AddToFriends].get)
      case Some(EventType(12)) => taskService ! TaskService.TaskEvent(self, message.decodeOption[AddEventScore].get)
      case Some(_) => log.info("Unknown message: {}", string.utf8String.trim)
      case None => log.info("Unknown message: {}", string.utf8String.trim)
    }
  }

  override def postStop() {
    log.info("Session stop: {}", toString)
  }
}

object Session {

  def props(connect: ActorRef) = Props(
    new Session(connect)
  )

}
