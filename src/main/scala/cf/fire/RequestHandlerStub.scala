package cf.fire

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import cf.conf.SimpleConf
import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http.{ChunkedMessageEnd, ChunkedRequestStart, MessageChunk, HttpRequest}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class RequestHandlerStub(producer: ActorRef) extends Actor with ActorLogging {

  log.debug("* * * * * RequestHandlerStub Start...")

  implicit val ec = this.context.dispatcher
  implicit val reqTimeout = {
    val dura = Duration(SimpleConf.getString("spray.can.server.request-timeout"))
    log.debug(s"reqest-timeout: ${dura.length} ${dura.unit}")
    Timeout.durationToTimeout(FiniteDuration(dura.length, dura.unit))
  }

  override def supervisorStrategy: SupervisorStrategy =
    SupervisorStrategy.stoppingStrategy

  // Spray's sender of request is not reachable remotely hence ChunkHandler
  // should live locally.
  override def receive: Receive = {
    case m: ChunkedRequestStart =>
      log.info("ChunkedRequestStart")
      val handler = context.actorOf(Props(classOf[ChunkHandler], producer,
        sender, m))
      sender ! RegisterChunkHandler(handler)

    case m: HttpRequest =>
      log.debug("HttpRequest: " + m)
      val parts = m.asPartStream()
      val handler = context.actorOf(Props(classOf[ChunkHandler], producer,
        sender, parts.head.asInstanceOf[ChunkedRequestStart]))
      parts.tail foreach {  handler ! _ }

    case m: Http.ConnectionClosed =>
      log.info("ConnectionClosed: " + m)
      context.stop(self)

    case m => log.warning("Unknown: " + m)
  }
}
