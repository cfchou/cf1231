package cf.kv

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Actor, ActorLogging}
import kafka.producer.KeyedMessage
import scala.reflect.runtime.universe.{TypeTag, typeOf}

object KvParserActor {
  case class RawMessage(json: String)
}

class KvParserActor(producer: ActorRef)
  extends Actor with KvParserSimple with ActorLogging {

  log.info("* * * * * KParserActor Start...")
  import KvParserActor.RawMessage

  type KM = KeyedMessage[String, String]

  override def receive: Receive = {
    case RawMessage(json) =>
      log.debug("KvParserActor: RawMessage")
      val msgs = parseMessages(json)
      val sqmsg = SeqMsg[KM](msgs, implicitly[TypeTag[KM]])
      producer ! sqmsg
      context.stop(self)
    case _ =>
      log.error("KvParserActor: Unknown")
  }

}
