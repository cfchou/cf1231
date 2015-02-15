package cf.kv

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Actor, ActorLogging}

object KvParserActor {
  case class RawMessage(json: String)
}

class KvParserActor(producer: ActorRef)
  extends Actor with KvParserSimple with ActorLogging {

  log.info("* * * * * KParserActor Start...")
  import KvParserActor.RawMessage

  override def receive: Receive = {
    case RawMessage(json) =>
      log.debug("KvParserActor: RawMessage")
      val msgs = parseMessages(json)
      producer ! msgs
      context.stop(self)
    case _ =>
      log.error("KvParserActor: Unknown")
  }

}
