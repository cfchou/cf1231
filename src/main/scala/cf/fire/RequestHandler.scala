package cf.fire

import akka.actor.{ActorLogging, Actor}
import spray.can.Http
import spray.http._

class RequestHandler extends Actor with ActorLogging {

  log.info("* * * * * RequestHandler Start...")

  override def receive: Receive = {
    case ChunkedRequestStart(req) =>
      log.info("ChunkedRequestStart")
    case MessageChunk(data, _) =>
      log.info("MessageChunk")
    case _: ChunkedMessageEnd =>
      log.info("ChunkedMessageEnd")
    case HttpRequest(HttpMethods.GET, uri, headers, entity, _) =>
      log.debug(s"GET $uri")
      sender ! HttpResponse(entity = "HEllo HEllo")
    case HttpRequest(HttpMethods.POST, uri, headers, entity, _) =>
      log.debug(s"POST $uri")
      sender ! HttpResponse(entity = "Ello Ello")
    case m : Http.ConnectionClosed =>
      log.info("ConnectionClosed: " + m)
      context.stop(self)
    case m => log.error("Unknown: " + m)
  }

}
