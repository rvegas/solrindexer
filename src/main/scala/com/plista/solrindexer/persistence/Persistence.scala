package com.plista.solrindexer.persistence

import java.nio.charset.Charset
import java.util.UUID

abstract class Persistence {

  def generateId(url: String) =
    UUID.nameUUIDFromBytes(url.getBytes(Charset.forName("UTF-8"))).toString

  def addArticle(urlarg: String, content: String): Unit
  def exists(url: String): Boolean
  def getText(url: String): String
}