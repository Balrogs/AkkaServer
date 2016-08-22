package aktor

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp._
import akka.io._

class MainActor(address: String, port: Int) extends Actor with ActorLogging {
  var idCounter = 0L

  override def preStart() {
    log.info("Starting tcp net server")

    import context.system
    val opts = List(SO.KeepAlive(on = true), SO.TcpNoDelay(on = true))
    IO(Tcp) ! Bind(self, new InetSocketAddress(address, port), options = opts)
  }

  override def postStop() {
    log.info("Stoping tcp net server")
  }

  def receive = {
    case b@Bound(localAddress) =>
    // do some logging or setup ...

    case CommandFailed(_: Bind) =>
      log.info("Command failed tcp server")
      context stop self

    case c@Connected(remote, local) =>
      log.info("New incoming tcp connection on server")
      createSession

    case _ => log.info("unknown message")
  }

  def createSession: Unit = {
    val connection = sender()
    val handler = context.actorOf(Session.props(connection))
    connection ! Register(handler)
  }
}

object MainActor {
  def props(address: String, port: Int) = Props(new MainActor(address, port))
}