package cf.fire

import java.io.BufferedReader

import akka.actor.Actor.Receive
import akka.actor._
import akka.io.{Tcp, IO}
import cf.conf.SimpleConf
import cf.kv.{KvParserSimple, KProducer}
import grizzled.slf4j.Logger
import spray.can.Http

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Try

object FireApp extends App {

  case object START
  case object STOP

  val log = Logger[this.type]
  val system = ActorSystem("FireApp")
  val runner = system.actorOf(Props(classOf[FireApp]))

  log.info("Start...")

  runner ! START

  val reader = Source.stdin.bufferedReader
  monitor(reader)
  reader.close()

  log.info("Stop...")
  Try { Await.ready(Promise[Unit].future, 5.seconds) }

  log.info("Shutdown...")
  system.scheduler.scheduleOnce(5.seconds) {
    system.shutdown();
  }

  def monitor(input: BufferedReader): Unit = {
    try {
      input.readLine() match {
        case m: String if "stop" == m.toLowerCase() => runner ! STOP
        case m =>
          log.debug(s"Unknown cmd: $m")
          monitor(input)
      }
    } catch {
      case e: Exception => log.error(e)
    }
  }
}



class FireApp extends Actor with ActorLogging {

  import FireApp.{START, STOP}

  log.info("* * * * * FireApp Start...")

  implicit val system = this.context.system

  // spray's HttpListener
  var listener: Option[ActorRef] = None

  lazy val start = {

   val producer = context.actorOf(Props(classOf[KProducer[String, String]],
      SimpleConf))

    // TODO: multiple interfaces/ports
    val inf = SimpleConf.getString("fire.interface")
    val prt = SimpleConf.getInt("fire.port")
    val handler = context.actorOf(Props(classOf[ConnectionListener], producer))

    log.info(s"Bind $inf:$prt")
    IO(Http) ! Http.Bind(handler, interface = inf, port = prt)
  }

  override def receive: Receive = {
    case START =>
      log.info("START")
      start
    case STOP =>
      log.info("STOP")
      listener.foreach(_ ! Http.Unbind)
    case m: Tcp.Bound =>
      log.info(s"Bound: $m")
      val s = sender()
      listener = Some(listener.fold { s } { _ =>
        // should never happen
        log.error("listener exists, will be overwritten")
        s
      })
    case m: Tcp.Unbound =>
      log.info(s"Unbound: $m")
      // TODO: DeathWatch
      context.stop(self)
    case m: Http.CommandFailed =>
      log.error(s"Bind failed $m")
      // TODO: DeathWatch
      context.stop(self)
    case m =>
      log.error("Unknown: " + m)
  }
}
