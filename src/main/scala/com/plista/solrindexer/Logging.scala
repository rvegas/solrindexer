package com.plista.solrindexer

import org.slf4j.LoggerFactory

trait Logging {
  val log = LoggerFactory.getLogger(this.getClass.getCanonicalName)
}
