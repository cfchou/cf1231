package cf.fire

import akka.actor.{ActorRef, ActorLogging, Actor}
import spray.can.Http
import spray.http._
import spray.io.CommandWrapper

import scala.concurrent.duration._

class ChunkHandler(client: ActorRef, start: ChunkedRequestStart)
  extends Actor with ActorLogging {

  import start.request

  implicit val system = this.context.system

  log.info("* * * * * RequestHandler Start...")

  // TODO: disable timeout?
  //client ! CommandWrapper(SetRequestTimeout(Duration.Inf))

  override def receive: Receive = {
    case MessageChunk(data, _) =>
      log.info(s"MessageChunk ${request.method} ${request.uri}: " +
        s"length=${data.length}")

    case _: ChunkedMessageEnd =>
      log.info(s"ChunkedMessageEnd ${request.method} ${request.uri}")

      sender ! HttpResponse(status = 200, entity = "h E l L o - H e L l O")

      // TODO: restore timeout?
      //client ! CommandWrapper(SetRequestTimeout(2.seconds))

      context.stop(self)
    case m => log.warning("Unknown: " + m)
  }
}
