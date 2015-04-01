package com.plista.solrindexer.sources

/**
 * Rabbit Configuration
 *
 * @param url
 * @param exchange
 * @param queue
 * @param routingkey
 * @param max_queue_len
 */
case class RabbitConfig(url: String, exchange: String,
                        queue: String, routingkey: String,
                        max_queue_len: Int)
