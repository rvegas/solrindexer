package com.plista.solrindexer.sources

import java.util.HashMap

import com.rabbitmq.client._

class RabbitReceiver(config: RabbitConfig) {

  val factory = new ConnectionFactory()
  factory.setUri(config.url)
  factory.setAutomaticRecoveryEnabled(true)
  factory.setRequestedHeartbeat(5)
  val connection = factory.newConnection()
  val channel = connection.createChannel()

  // Additional queue params
  val qArgs = new HashMap[String, Object]()
  val length = config.max_queue_len
  qArgs.put("x-max-length", new Integer(length))

  channel.queueDeclare(config.queue, true, false, false, qArgs)
  channel.queueBind(config.queue, config.exchange, config.routingkey)
  val consumer = new QueueingConsumer(channel)
  channel.basicConsume(config.queue, true, "SolrIndexer", consumer)
}
