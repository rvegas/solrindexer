package com.plista.solrindexer.parsing

case class ItemUpdateCreate(domain: Int, itemid: String,
                      title: String, created_at: String,
                      text: String, categories: Seq[String],
                      annotations: Seq[String],
                      fulltext: String, weight: Int)
