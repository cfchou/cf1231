package cf.conf

import java.util.Properties

import com.typesafe.config.{ConfigFactory, Config}
import kafka.producer.{Producer, ProducerConfig}

object SimpleConf {
  type SimpleConf = this.type

  implicit def toConfig[K, V](cf: SimpleConf): Config = cf.conf

  implicit def toProducerConfig[K, V](cf: SimpleConf): ProducerConfig =
    cf.producer_conf

  lazy val conf = ConfigFactory.load

  lazy val producer_conf = new ProducerConfig({
    val props = new Properties

    props.put("metadata.broker.list", conf.getString("metadata.broker.list"))
    props.put("request.required.acks",
      conf.getInt("request.required.acks").toString)
    props.put("producer.type", conf.getString("producer.type"))
    props.put("serializer.class", conf.getString("serializer.class"))
    props
  })
}

