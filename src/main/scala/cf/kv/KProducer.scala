package cf.kv

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import cf.conf.SimpleConf.SimpleConf
import kafka.producer.{KeyedMessage, ProducerConfig, Producer}

import scala.reflect.runtime.universe.{TypeTag, typeOf}

case class SeqMsg[U](ms: Seq[U], tag: TypeTag[U])

object KProducer {
  def props[K:TypeTag, V:TypeTag](conf: SimpleConf) = {
    Props(new KProducer[K, V](conf))
  }
}

class KProducer[K:TypeTag, V:TypeTag](conf: SimpleConf)
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
    case SeqMsg(ms, tag) if tag.tpe <:< typeOf[KeyedMessage[K, V]] =>
      // Seq[_] rather than Seq[KeyedMessage[K, V]] is seen at runtime. But
      // since the type is checked in case clause, no cast exception would
      // happen.
      // We still need to make static type check for send, therefore we can't
      // write Seq[_] or Seq[KeyedMessage[_, _]]
      val tmp = ms.asInstanceOf[Seq[KeyedMessage[K, V]]]
      producer.send(tmp: _*)
    case _ =>
      log.error("Unknown")
  }
}




