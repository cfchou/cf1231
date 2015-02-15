package cf.kv

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import cf.conf.SimpleConf.SimpleConf
import kafka.producer.{KeyedMessage, ProducerConfig, Producer}


class KProducer[K, V](conf: SimpleConf)
  extends Actor with ActorLogging {

  log.info("* * * * * KProducer Start...")

  val producer = new Producer[K, V](conf)

  @throws(classOf[Exception])
  override def preStart(): Unit = super.preStart()

  @throws(classOf[Exception])
  override def postStop(): Unit = {
    log.info("KProducer postStop...")
    super.postStop()
    producer.close()
  }

  override def receive: Receive = {
    case m: KeyedMessage[K, V] =>
      producer.send(m)
    case ms: Seq[KeyedMessage[K, V]] =>
      // FIXME: uncheck type warning
      producer.send(ms: _*)
    /*
    case Seq(a, rest@ _*) if a.isInstanceOf[KeyedMessage[K, V]] =>
      producer.send(rest)
    */
    case _ =>
      log.error("Unknown")
  }

}
