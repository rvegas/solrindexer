name := "solr indexer"

version := "0.0.1"

scalaVersion := "2.10.4"

organization := "com.plista"

homepage := Some(url("http://www.plista.com"))

description := "Solr Indexer"

parallelExecution in Test := false

resolvers ++= Seq(
  "plista Nexus" at "https://nexus.plista.com/content/groups/public/",
"googlecode" at "http://boilerpipe.googlecode.com/svn/repo/"
)

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "com.codahale.metrics" % "metrics-core" % "3.0.2",
  "com.codahale.metrics" % "metrics-graphite" % "3.0.2",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.play" % "play-json_2.10" % "2.4.0-M2",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "org.graylog2" % "gelfj" % "1.1.7",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.rabbitmq" % "amqp-client" % "3.4.3",
  "org.apache.solr" % "solr-solrj" % "5.0.0",
  "log4j" % "log4j" % "1.2.15",
  "commons-logging" % "commons-logging" % "1.2",
  "commons-codec" % "commons-codec" % "1.10"
)
