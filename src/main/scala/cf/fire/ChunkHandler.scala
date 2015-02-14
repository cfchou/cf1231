package cf.fire

import java.util.Properties

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.{ByteString, ByteStringBuilder}
import cf.conf.{SimpleConf}
import cf.kv.KvParserSimple
import com.typesafe.config.Config
import kafka.producer.{Producer, ProducerConfig}
import spray.can.Http
import spray.http._
import spray.io.CommandWrapper

import scala.concurrent.Future
import scala.concurrent.duration._

class ChunkHandler(conf: Config, client: ActorRef, start: ChunkedRequestStart)
  extends Actor with ActorLogging {

  import start.request

  log.info("* * * * * ChunkHandler Start...")

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

      // TODO: restore timeout?
      // client ! CommandWrapper(SetRequestTimeout(conf.getInt(
      //   "spray.can.server.request-timeout")
      val parser = KvParserSimple()
      val msgs = parser.parseMessages(s)
      val producer = new SimpleConf {}.newProducer[String, String](conf)
      msgs.foreach( producer.send(_) )
      producer.close()

      context.stop(self)
    case m => log.warning("Unknown: " + m)
  }
}

