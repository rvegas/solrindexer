package com.plista.solrindexer.persistence

case class SolrItem(
        itemid: String,
        domainid: String,
        title: String,
        text: String,
        category: Seq[String],
        text_full: String,
        date: String,
        entities_sce: Seq[String]
)
