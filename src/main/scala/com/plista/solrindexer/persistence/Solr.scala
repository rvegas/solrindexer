package com.plista.solrindexer.persistence

import java.text.SimpleDateFormat
import java.util.Date

import com.plista.solrindexer.{SolrIndexerService, Logging}
import com.plista.solrindexer.parsing.ItemUpdateCreate
import dispatch.Defaults._
import dispatch._
import com.ning.http.client.Response
import play.api.libs.json.{JsObject, JsString, JsArray, Json}

import scala.util.{Failure, Success, Try}
import scala.util.parsing.json.{JSONArray, JSONObject}

class Solr(config: SolrConfig) extends Logging {

  val metrics = SolrIndexerService.metrics
  val meterIndexed = metrics.meter("indexed")
  val meterErrorsCreate = metrics.meter("errors-create")
  val meterDeleted = metrics.meter("deleted")
  val meterErrorsDelete = metrics.meter("errors-delete")

  // initializing the connection pool
  val myHttp = Http.configure(_
    .setAllowPoolingConnection(true)
    .setAllowSslConnectionPool(true)
    .setConnectionTimeoutInMs(2000)
    .setMaxConnectionLifeTimeInMs(2000)
    .setUserAgent("Plista Datafication")
    .setCompressionEnabled(true)
    .setFollowRedirects(true)
    .setMaximumConnectionsTotal(100)
    .setMaximumConnectionsPerHost(100))

  val username = config.username
  val password = config.password
  val auth = new sun.misc.BASE64Encoder().encode((username + ":" + password).getBytes)
  val updateurl = "/update?wt=json"
  val commiturl = "&commit=true"

  def commit: Unit = {
    val svc = url(config.url + updateurl + commiturl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Accept", "application/json")
      .setHeader("Authorization", "Basic " + auth)

    val response = myHttp(svc)
    for (res <- response) {
    }
  }

  def addArticle(doc: ItemUpdateCreate): Unit = {
    val categoryObject = JsArray(doc.categories.map { category => Json.toJson(category)})
    val annotationObject = JsArray(doc.annotations.map { entity => Json.toJson(entity)})
    val formatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val inputFormatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val date_index = formatDate.format(new Date)
    val date_created = formatDate.format(inputFormatDate.parse(doc.created_at))

    val articleJson = Json.stringify(Json.toJson(Map(
      "add" -> Json.toJson(Map(
        "doc" -> JsObject(Map(
          "itemid" -> Json.toJson(doc.itemid),
          "text" -> Json.toJson(doc.text),
          "domainid" -> Json.toJson(doc.domain),
          "title" -> Json.toJson(doc.title),
          "text" -> Json.toJson(doc.text),
          "category" -> categoryObject,
          "entities_sce" -> annotationObject,
          "text_full" -> Json.toJson(doc.fulltext),
          "category_plista" -> Json.toJson("fromscala"),
          "date_i" -> Json.toJson(date_index),
          "date" -> Json.toJson(date_created)
        ).toSeq
        ))
      ))
    ))

    val svc = url(config.url + updateurl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Authorization", "Basic " + auth)
      .POST
      .setBody(articleJson)
      .setBodyEncoding("UTF-8")

    val response = myHttp(svc)
    for (res <- response) {
      meterIndexed.mark()
    }

    //TODO: remove me when solr5 is fully migrated
    val svcsolr5 = url(config.solr5url + updateurl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Authorization", "Basic " + auth)
      .POST
      .setBody(articleJson)
      .setBodyEncoding("UTF-8")

    val responsesolr5 = myHttp(svcsolr5)
    for (res <- responsesolr5) {
    }
    //TODO: remove until here
  }

  /**
   *
   * @param docs
   */
  def addArticleBatch(docs: List[ItemUpdateCreate]): Unit = {
    for (doc <- docs) {

      val categoryObject = JsArray(doc.categories.map {
        category => Json.toJson(category)
      })
      val annotationObject = JsArray(doc.annotations.map {
        entity => Json.toJson(entity)
      })
      val formatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
      val inputFormatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val date_index = formatDate.format(new Date)
      val date_created = formatDate.format(inputFormatDate.parse(doc.created_at))

      val articleJson = Json.stringify(Json.toJson(Map(
        "add" -> Json.toJson(Map(
          "doc" -> JsObject(Map(
            "itemid" -> Json.toJson(doc.itemid),
            "text" -> Json.toJson(doc.text),
            "domainid" -> Json.toJson(doc.domain),
            "title" -> Json.toJson(doc.title),
            "text" -> Json.toJson(doc.text),
            "category" -> categoryObject,
            "entities_sce" -> annotationObject,
            "text_full" -> Json.toJson(doc.fulltext),
            "category_plista" -> Json.toJson("fromscala"),
            "date_i" -> Json.toJson(date_index),
            "date" -> Json.toJson(date_created)

          ).toSeq
          ))
        ))
      ))

      val svc = url(config.url + updateurl)
        .setHeader("Accept-Charset", "UTF-8")
        .setHeader("Content-Type", "application/json")
        .setHeader("Authorization", "Basic " + auth)
        .POST
        .setBody(articleJson)
        .setBodyEncoding("UTF-8")

      val response = myHttp(svc)
      for (res <- response) {
        meterIndexed.mark()
        res
      }

      //TODO: remove me when solr5 is fully migrated
      val svcsolr5 = url(config.solr5url + updateurl)
        .setHeader("Accept-Charset", "UTF-8")
        .setHeader("Content-Type", "application/json")
        .setHeader("Authorization", "Basic " + auth)
        .POST
        .setBody(articleJson)
        .setBodyEncoding("UTF-8")

      val responsesolr5 = myHttp(svcsolr5)
      for (res <- responsesolr5) {
        responsesolr5
      }
      //TODO: remove until here
    }

    val svc = url(config.url + updateurl + commiturl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Accept", "application/json")
      .setHeader("Authorization", "Basic " + auth)

    val response = myHttp(svc)
    for (res <- response) {
      meterIndexed.mark()
    }

    //TODO: remove me when solr5 is fully migrated
    val svcsolr5 = url(config.solr5url + updateurl + commiturl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Accept", "application/json")
      .setHeader("Authorization", "Basic " + auth)

    val responsesolr5 = myHttp(svcsolr5)
    for (res <- responsesolr5) {
    }
    //TODO: remove until here
  }


  def delete(itemid: String): Try[Unit] = {
    val deleteJson = Json.stringify(Json.toJson(Map(
      "delete" -> Json.toJson(Map(
        "query" -> Json.toJson("itemid:" + itemid)
      )
      ))
    ))

    //TODO: remove me when solr5 is fully migrated
    val svcsolr5 = url(config.solr5url + updateurl + commiturl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Accept", "application/json")
      .setHeader("Authorization", "Basic " + auth)
      .POST
      .setBody(deleteJson)
      .setBodyEncoding("UTF-8")

    val responsesolr5 = myHttp(svcsolr5)
    for (res <- responsesolr5) {
    }
    //TODO: remove until here

    val svc = url(config.url + updateurl + commiturl)
      .setHeader("Accept-Charset", "UTF-8")
      .setHeader("Content-Type", "application/json")
      .setHeader("Accept", "application/json")
      .setHeader("Authorization", "Basic " + auth)
      .POST
      .setBody(deleteJson)
      .setBodyEncoding("UTF-8")

    val response: Either[Throwable, Response] = myHttp(svc).either()

    response match {
      case Right(res) =>
        meterDeleted.mark()
        Success()
      case Left(e) =>
        log.error("error when deleting " + e.getMessage)
        meterErrorsDelete.mark()
        Failure(new Exception(e.getMessage))
    }
  }
}
