package cf.fire

import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.typesafe.config.Config
import spray.http.{ChunkedMessageEnd, ChunkedRequestStart, MessageChunk, HttpRequest}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

// A gate for ALL requests through the same connection. If a request is not
// chunked, it'll be dealt with by one actor. For chunked requests that form a
// big request, they are handled by one actor.
class RequestHandlerStub(conf: Config) extends Actor with ActorLogging {

  log.debug("* * * * * RequestHandlerStub Start...")

  implicit val system = this.context.system
  implicit val ec = this.context.dispatcher
  implicit val reqTimeout = {
    val dura = Duration(conf.getString("spray.can.server.request-timeout"))
    log.debug(s"reqest-timeout: ${dura.length} ${dura.unit}")
    Timeout.durationToTimeout(FiniteDuration(dura.length, dura.unit))
  }

  override def receive: Receive = normalReceive

  def normalReceive: Receive = {

    case m: ChunkedRequestStart =>
      log.info("ChunkedRequestStart")
      restartChunkedReceive(None, m)

    case req: HttpRequest =>
      log.debug("HttpRequest: " + req)
      // TODO: test if generalizing to a remote actor works
      val s = sender()
      val handler = system.actorOf(Props[RequestHandler])

      // instead of 'ask', 'tell' and allow RequnestHandler to directly response
      // to spary's connection actor
      //handler ? req pipeTo(s)
      handler.tell(req, s)
  }

  def chunkedReceive(ckHandler: ActorRef): Receive = {
    case m: MessageChunk =>
      log.info("MessageChunk")
      ckHandler.tell(m, sender())

    case m: ChunkedMessageEnd =>
      log.info("ChunkedMessageEnd")
      ckHandler.tell(m, sender())
      context.become(normalReceive)

    // A chunked request should not be interleaved with other requests
    case m: ChunkedRequestStart =>
      log.error("another ChunkedRequestStart")
      restartChunkedReceive(Some(ckHandler), m)

    case req: HttpRequest =>
      log.error("unfinished chunked request")
      context.stop(ckHandler)
      context.become(normalReceive)
      normalReceive(req)
  }

  def restartChunkedReceive(old: Option[ActorRef], req: ChunkedRequestStart) = {
    def newChunkedReceive = {
      val handler = system.actorOf(Props[RequestHandler])


      // FIXME:
      // use 'tell' partly because we don't know if RequestHandler would return
      // chunked responses or just one single response at the end.
      //
      // there're 2 caveats in 'tell'
      // 1. this might not matter: spary's connection actor see the response
      // from the sender(RequestHandler) different to what it sent the request.
      // to(RquestHandlerStub).
      // 2. RequestHandler has to be in the same JVM  as the connection actor
      //
      // 'ask' might solve both but would cause all chunks arriving out of
      // order. we can solve that by sequence multiple futures:
      // case Start => f = handler ? Start pipeTo sender()
      // case Chunk => f = f map { _ => handler ? Chunk pipeTo sender() }
      // ......
      //
      // or introduce FSM

      handler.tell(req, sender())
      context.become(chunkedReceive(handler))
    }
    old match {
      case Some(ckHandler) =>
        context.stop(ckHandler)
        newChunkedReceive
      case None =>
        newChunkedReceive
    }
  }
}
