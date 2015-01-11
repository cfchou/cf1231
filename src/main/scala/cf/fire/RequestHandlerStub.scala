package cf.fire

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.typesafe.config.Config
import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http.{ChunkedMessageEnd, ChunkedRequestStart, MessageChunk, HttpRequest}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class RequestHandlerStub(conf: Config) extends Actor with ActorLogging {

  log.debug("* * * * * RequestHandlerStub Start...")

  implicit val system = this.context.system
  implicit val ec = this.context.dispatcher
  implicit val reqTimeout = {
    val dura = Duration(conf.getString("spray.can.server.request-timeout"))
    log.debug(s"reqest-timeout: ${dura.length} ${dura.unit}")
    Timeout.durationToTimeout(FiniteDuration(dura.length, dura.unit))
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => Stop
  }

  override def receive: Receive = {
    case m: ChunkedRequestStart =>
      log.info("ChunkedRequestStart")
      val handler = system.actorOf(Props(classOf[ChunkHandler], conf, sender,
        m))
      sender ! RegisterChunkHandler(handler)

    case m: HttpRequest =>
      log.debug("HttpRequest: " + m)
      val parts = m.asPartStream()
      val handler = system.actorOf(Props(classOf[ChunkHandler], conf, sender,
        parts.head.asInstanceOf[ChunkedRequestStart]))
      parts.tail foreach {  handler ! _ }

    case m: Http.ConnectionClosed =>
      log.info("ConnectionClosed: " + m)
      context.stop(self)

    case m => log.warning("Unknown: " + m)
  }
}
