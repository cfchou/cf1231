package cf.fire

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import spray.can.Http

// one for one bound interface. its job is to create actors to handle incoming
// connections.
class ConnectionListener(producer: ActorRef) extends Actor with ActorLogging {

  log.debug("* * * * * ConnectionListener Start......")

  override def receive: Receive = {
    case Http.Connected(remote, _) =>
      log.debug(s"connect from ${remote.getAddress}:${remote.getPort}")
      // RequestHandler directly talks to Spray's connection actor hence can't
      // not be remote.
      val stub = context.actorOf(Props(classOf[RequestHandlerStub], producer))
      sender ! Http.Register(stub)
    case m => log.error("ConnectionListener: Unknown: " + m)
  }

}
