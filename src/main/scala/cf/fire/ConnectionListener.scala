package cf.fire

import akka.actor.{Props, ActorLogging, Actor}
import com.typesafe.config.Config
import spray.can.Http

class ConnectionListener(conf: Config) extends Actor with ActorLogging {

  log.debug("* * * * * ConnectionListener Start......")

  implicit val system = this.context.system

  //
  override def receive: Receive = {
    case Http.Connected(remote, _) =>
      log.debug(s"connect from ${remote.getAddress}:${remote.getPort}")
      val stub = system.actorOf(Props(classOf[RequestHandlerStub], conf))
      sender ! Http.Register(stub)
    case m => log.error("Unknown: " + m)
  }

}
