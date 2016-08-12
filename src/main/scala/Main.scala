import akka.actor._
import aktor._
import aktor.gm.GameService

object Main extends App {
  val system = ActorSystem("system")
  val taskActor = system.actorOf(Props[TaskService], "task")
  val gmActor = system.actorOf(Props[GameService], "game")
  val mainActor = system.actorOf(MainActor.props("192.168.211.97",8888))
  system.awaitTermination()
}