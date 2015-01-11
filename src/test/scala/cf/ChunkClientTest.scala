package cf

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorSystem, Actor, ActorLogging}
import akka.io.IO
import com.typesafe.config.{Config, ConfigFactory}
import spray.can.Http
import spray.http._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import grizzled.slf4j.Logger

object ChunkClientTest extends App {

  val log = Logger[this.type]

  implicit val system = ActorSystem("ChunkClientTest")
  val conf = ConfigFactory.load
  system.actorOf(Props(classOf[ChunkClientTest], conf))

  log.info("Shutdown...")
  system.scheduler.scheduleOnce(10.seconds) {
    system.shutdown();
  }


}

class ChunkClientTest(conf: Config) extends Actor with ActorLogging {

  log.info("* * * * * ChunkClientTest Start...")

  implicit val system = this.context.system

  val inf = conf.getString("fire.interface")
  val prt = conf.getInt("fire.port")

  IO(Http) ! Http.Connect(inf, prt)

  override def receive: Receive = {
    case _: Http.Connected =>
      log.info("Connected")
      // once connected, we can send the request across the connection
      val peer = sender()
      peer ! ChunkedRequestStart(HttpRequest(HttpMethods.POST, "/"))
      chuncks foreach { c =>
        log.info(s"chunk size: ${c.data.length}")
        peer ! c
      }
      peer ! ChunkedMessageEnd

    case Http.CommandFailed(Http.Connect(address, _, _, _, _)) =>
      log.warning("Could not connect to {}", address)
      context.stop(self)

    case m =>
      log.info(s"Unknown: $m")
  }


  def chuncks: List[MessageChunk] = {
    List(MessageChunk("abc asd \n 123"),
      MessageChunk("=============="),
      MessageChunk("000000000000098887"))
  }
}


