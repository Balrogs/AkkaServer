import akka.actor._
import aktor._
import aktor.gm.GameService
import global.server.MongoDBDriver


object Main extends App {

  override def main(args: Array[String]) {
    args.length match {
      case 2 =>
        val system = ActorSystem("arcunlim")
        MongoDBDriver.setHost(args(0))
        val taskActor = system.actorOf(Props[TaskService], "task")
        val gmActor = system.actorOf(Props[GameService], "game")
        val mainActor = system.actorOf(MainActor.props(args(0), args(1).toInt))
        system.awaitTermination()
      case _ => println("Usage akkaserver <host> <port>")
    }
  }
}