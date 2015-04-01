package com.plista.solrindexer.parsing

import com.plista.solrindexer.Logging
import play.api.libs.json.{Json, JsValue}
import scala.util.{Try, Failure, Success}

class Parser extends Logging {

  /**
   * Parse a ItemUpdate json from the delete stream
   *
   * @param message
   * @return
   */
  def parseDelete(message: String): Try[ItemUpdateDelete] = {
    val json = Json.parse(message)

    val domain = readDomainId(json)
    if (domain.isFailure) {
      val msg = "Domain not specified - delete"
      log.error(msg + ": " + message)
      return Failure(new Exception(msg))
    }

    val itemid = (json \ "itemid").asOpt[String]
    if (itemid.isEmpty) {
      val msg = "ItemId not specified - delete"
      log.error(msg)
      return Failure(new Exception(msg))
    }

    val weight = readWeight(json)
    if (weight.isFailure) {
      val msg = "Weight not specified - delete"
      log.error(msg)
      return Failure(new Exception(msg))
    }

    Success(ItemUpdateDelete(domain.get, itemid.get, weight.get))
  }

  /**
   * Read the domainid from the JSON
   * Sometimes it is of type String, sometimes it is of type Int
   *
   * @param json
   * @return
   */
  def readDomainId(json: JsValue): Try[Int] = {
    val jsValue = json \ "domainid"
    val strValue = jsValue.asOpt[String] // try as string

    strValue match {
      case Some(strVal) => Success(strVal.toInt)
      case None =>
        val intValue = jsValue.asOpt[Int] // try as integer
        intValue match {
          case Some(intVal) => Success(intVal)
          case None => Failure(new Exception("domain does not exist"))
        }
    }
  }

  /**
   * Read the weight from the JSON
   * Sometimes it is of type String, sometimes it is of type Int
   *
   * @param json
   * @return
   */
  def readWeight(json: JsValue): Try[Int] = {
    val jsValue = json \ "weight"
    val strValue = jsValue.asOpt[String] // try as string

    strValue match {
      case Some(strVal) => Success(strVal.toInt)
      case None =>
        val intValue = jsValue.asOpt[Int] // try as integer
        intValue match {
          case Some(intVal) => Success(intVal)
          case None => Failure(new Exception("weight does not exist"))
        }
    }
  }

  /**
   * Parse a ItemUpdate json from the create stream
   *
   * @param message
   * @return
   */
  def parseCreate(message: String): Try[ItemUpdateCreate] = {

    val json = Json.parse(message) match {
      case null =>
        val msg = "Could not parse JsVal"
        log.error(msg)
        return Failure(new Exception(msg))
      case jsVal: JsValue => jsVal
    }

    val parameterNames = Array("itemid", "item",
      "created_at", "text")

    val pairs = parameterNames.map {
      parameter =>
        (parameter, extractParameter(json, parameter))
    }.map {
      case (parameter, either) =>
        either.fold(
        { failure => return failure}, { result => (parameter, result)})
    }
    val parameters = pairs.toMap[String, String]

    // special cases
    val weight = readWeight(json \ "original")
    if (weight.isFailure) {
      val msg = "Weight not specified"
      log.error(msg)
      return Failure(new scala.Exception(msg))
    }

    val domainId = readDomainId(json \ "original")
    if(domainId.isFailure) {
      val msg = "Domain not specified"
      log.error(msg)
      return Failure(new scala.Exception(msg))
    }

    val fulltext = (json \ "fulltext").asOpt[String]
    if (fulltext.isEmpty) {
      val msg = "Fulltext not specified"
      log.error(msg)
      return Failure(new scala.Exception(msg))
    }

    val categories = prepareCategories(json \ "categories")
    val annotations = prepareAnnotations(json \ "annotations")

    Success(ItemUpdateCreate(domainId.get,
      (parameters get "itemid").get,
      (parameters get "item").get,
      (parameters get "created_at").get,
      (parameters get "text").get,
      categories,
      annotations,
      fulltext.get,
      weight.get))
  }

  /**
   * prepare the category list to get top 10 entities
   *
   * @param categories list of names obtained from semantics service as entities
   * @return
   */
  def prepareCategories(categories: JsValue): Seq[String] = {
    val names = categories \\ "category"
    names.slice(0, 10).map(_.as[String]).map(_.replace(":", " "))
  }

  /**
   * prepare the annotations for solr to be saved in entities_sce
   *
   * @param annotations list of annotations from semantics service
   * @return
   */
  def prepareAnnotations(annotations: JsValue): Seq[String] = {
    val names = annotations \\ "name"
    names.map(_.as[String]).map(_.replace(":", " "))
  }

  /**
   * Used method for extracting a parameter
   *
   * @param json
   * @param parName
   * @return
   */
  private def extractParameter(json: JsValue, parName: String):
  Either[Failure[ItemUpdateCreate], String] = {
    val extracted = (json \ "original" \ parName).asOpt[String]
    if (extracted.isEmpty) {
      val msg = s"$parName not specified - create"
      log.error(msg)
      return Left(Failure(new scala.Exception(msg)))
    }
    Right(extracted.get)
  }
}
