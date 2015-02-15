package cf.kv

import kafka.producer.KeyedMessage

trait KvParser[K, V] {
  def parseMessages(content: String): Seq[KeyedMessage[K, V]]
}
