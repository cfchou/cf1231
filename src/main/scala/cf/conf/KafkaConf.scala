package cf.conf

import com.typesafe.config.Config
import kafka.producer.Producer

trait KafkaConf {

  def newProducer[K, V](conf: Config): Producer[K, V]

}
