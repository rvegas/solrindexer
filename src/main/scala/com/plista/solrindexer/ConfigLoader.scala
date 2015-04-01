package com.plista.solrindexer

import java.io.File

import com.plista.solrindexer.persistence.SolrConfig
import com.plista.solrindexer.sources.RabbitConfig
import com.typesafe.config.ConfigFactory

object ConfigLoader {
  val appconfig = ConfigFactory.parseFile(new File("application.conf"))
  val testconfig = ConfigFactory.parseFile(new File("application-template.conf"))
  var isTest = false

  def setTest() = {
    isTest = true
  }

  def getConfig = {
    if(isTest)
      testconfig
    else
      appconfig
  }

  /**
   * Return the RabbitMQ configuration
   *
   * @return
   */
  lazy val getRabbitConfigCreate: RabbitConfig = {
    val config = getConfig
    new RabbitConfig(
      config.getString("rabbit_create.url"),
      config.getString("rabbit_create.exchange"),
      config.getString("rabbit_create.queue"),
      config.getString("rabbit_create.routing_key"),
      config.getInt("rabbit_create.max_queue_length"))
  }

  /**
   * Return the RabbitMQ configuration
   *
   * @return
   */
  lazy val getRabbitConfigDelete: RabbitConfig = {
    val config = getConfig
    new RabbitConfig(
      config.getString("rabbit_delete.url"),
      config.getString("rabbit_delete.exchange"),
      config.getString("rabbit_delete.queue"),
      config.getString("rabbit_delete.routing_key"),
      config.getInt("rabbit_delete.max_queue_length"))
  }

  /**
   * Return Solr configuration
   */
  lazy val getSolrConfig: SolrConfig = {
    val config = getConfig
    SolrConfig(
      config.getString("solr.url"),
      config.getString("solr.username"),
      config.getString("solr.password"),
      config.getString("solr.solr5url"))
  }
}
