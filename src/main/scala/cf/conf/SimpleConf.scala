package cf.conf

import java.util.Properties

import com.typesafe.config.Config
import kafka.producer.{Producer, ProducerConfig}

trait SimpleConf extends KafkaConf {

  override def newProducer[K, V](conf: Config): Producer[K, V] = {
    val props = new Properties

    props.put("metadata.broker.list", conf.getString("metadata.broker.list"))
    props.put("request.required.acks",
      conf.getInt("request.required.acks").toString)
    props.put("producer.type", conf.getString("producer.type"))
    props.put("serializer.class", conf.getString("serializer.class"))

    new Producer[K, V](new ProducerConfig(props))
  }
}
