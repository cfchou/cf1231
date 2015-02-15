package cf.fire

import java.util.Properties

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.{ByteString, ByteStringBuilder}
import cf.conf.{SimpleConf}
import cf.kv.KvParserActor.RawMessage
import cf.kv.{KvParserActor, KProducer, KvParser, KvParserSimple}
import com.typesafe.config.Config
import kafka.producer.{Producer, ProducerConfig}
import spray.can.Http
import spray.http._
import spray.io.CommandWrapper

import scala.concurrent.Future
import scala.concurrent.duration._

// @client: Spray's sender of request, not reachable remotely
class ChunkHandler(producer: ActorRef, client: ActorRef, start: ChunkedRequestStart)
  extends Actor with ActorLogging {

  import start.request

  log.info("* * * * * ChunkHandler Start...")

  override def supervisorStrategy: SupervisorStrategy =
    SupervisorStrategy.stoppingStrategy

  // TODO: disable timeout?
  // client ! CommandWrapper(SetRequestTimeout(Duration.Inf))

  override def receive: Receive = received(ByteString.empty)

  @throws(classOf[Exception])
  override def postStop(): Unit = {
    log.info("STOP !!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    super.postStop()
  }

  def received(rest: ByteString): Receive = {
    case MessageChunk(data, _) =>
      log.info(s"MessageChunk ${request.method} ${request.uri}: " +
        s"length=${data.length}")

      //data.asString(HttpCharsets.`UTF-8`)
      context.become(received(rest ++ data.toByteString))

    case _: ChunkedMessageEnd =>
      log.info(s"ChunkedMessageEnd ${request.method} ${request.uri}")

      val s = rest.decodeString(HttpCharsets.`UTF-8`.toString)
      // response with the length of the string
      client ! HttpResponse(status = 200, entity = s.length.toString)

      val parser = context.actorOf(Props(classOf[KvParserActor], producer))
      parser ! RawMessage(s)
      context.watch(parser)

      // TODO: restore timeout?
      // client ! CommandWrapper(SetRequestTimeout(conf.getInt(
      //   "spray.can.server.request-timeout")

    case Terminated(parser) =>
      log.info(parser.path.name + " died .....")
      context.stop(self)
    case m => log.warning("Unknown: " + m)
  }
}

