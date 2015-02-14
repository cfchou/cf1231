package cf.fire

import akka.actor.{Props, ActorLogging, Actor}
import com.typesafe.config.Config
import spray.can.Http

// one for one bound interface. its job is to create actors to handle incoming
// connections.
class ConnectionListener(conf: Config) extends Actor with ActorLogging {

  log.debug("* * * * * ConnectionListener Start......")

  override def receive: Receive = {
    case Http.Connected(remote, _) =>
      log.debug(s"connect from ${remote.getAddress}:${remote.getPort}")
      val stub = context.actorOf(Props(classOf[RequestHandlerStub], conf))
      sender ! Http.Register(stub)
    case m => log.error("ConnectionListener: Unknown: " + m)
  }

}
