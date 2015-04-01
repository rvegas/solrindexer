package com.plista.solrindexer

import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{MetricRegistry, MetricFilter}
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import com.plista.solrindexer.persistence.{SolrItem, Solr}
import com.plista.solrindexer.sources.RabbitReceiver
import com.plista.solrindexer.parsing.{ItemUpdateCreate, Parser}
import com.rabbitmq.client.QueueingConsumer.Delivery
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object SolrIndexerService extends Logging {
  val metrics = new MetricRegistry()
  val meterErrorsGeneral = metrics.meter("errors-parse")

  try {
    val graphiteServer = "plista328.plista.com"
    val graphite = new Graphite(new InetSocketAddress(graphiteServer, 2003))
    val reporter = GraphiteReporter.forRegistry(metrics)
      .prefixedWith("plista.data_extraction.solr_indexer")
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(1, TimeUnit.MINUTES)
  } catch {
    case ex: Throwable =>
      log.error("Error start reporting")
      ex.printStackTrace()
  }

  // intialize rabbit
  val createReceiver = new RabbitReceiver(ConfigLoader.getRabbitConfigCreate)
  val deleteReceiver = new RabbitReceiver(ConfigLoader.getRabbitConfigDelete)
  val parser = new Parser()

  val solr = new Solr(ConfigLoader.getSolrConfig)

  // These are the weights which are indexed
  val validCreateWeights = Array(0, 4)

  /**
   * Consume a delivery from the delete stream
   * HPTStream where weight == 8
   */
  def consumeDeleteStream() = {
    // get item from HPTStream
    val delivery = deleteReceiver.consumer.nextDelivery()
    val message = new String(delivery.getBody)

    parser.parseDelete(message) match {
      case Success(itemUpdate) =>
        itemUpdate.weight match {
          case 8 => solr.delete(itemUpdate.itemid)
          case _ =>
        }
      case Failure(err) =>
        log.error(err.getMessage + " in message with routing key" +
          delivery.getEnvelope.getRoutingKey)
        meterErrorsGeneral.mark()
    }
  }

  /**
   * Consume a create delivery and process it to solr in batch mode
   */
  def consumeCreateStream() = {
    var createCounter = 0
    val delivery = createReceiver.consumer.nextDelivery()
    val doc = parseDelivery(delivery) match {
      case Success(doc) =>
        if (validCreateWeights.contains(doc.weight))
          createCounter += 1
        solr.addArticle(doc)
        if (createCounter == 1000) {
          solr.commit
          createCounter = 0
        }
      case _ =>
    }
  }

  /**
   * Parse the delivery message - if its not a create message we don't forward to solr, otherwise we form a SolrItem
   * and feed it to solr
   *
   * @param delivery delivery object from rabbitmq
   * @return
   */
  def parseDelivery(delivery: Delivery): Try[ItemUpdateCreate] = {
    val message = new String(delivery.getBody)

    val itemUpdate = parser.parseCreate(message) match {
      case Success(update) => update
      case Failure(error) =>
        log.error(error.getMessage + "in " + message)
        meterErrorsGeneral.mark()
        return Failure(error)
    }
    Success(itemUpdate)
  }

  /**
   * Main method
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    Future {
      while (true) {
        consumeCreateStream()
      }
    }
    Future {
      while (true) {
        consumeDeleteStream()
      }
    }
  }


}
