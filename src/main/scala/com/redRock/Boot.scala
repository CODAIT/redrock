package com.redRock

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

object Boot extends App {
	
	// Change to a more reasonable default number of partitions (from 200)
	SparkContVal.sqlContext.setConf("spark.sql.shuffle.partitions", s"${Config.numberOfPartitions}")
	SparkContVal.sqlContext.setConf("spark.sql.codegen", "false")

	println("Registering analysis function")
	AnalysisFunction.registerAnalysisFunctions()
	//println("Caching words2vec")
	//LoadWords2Vec.words2vec.cache()
	//println(s"Words2Vec ===> ${LoadWords2Vec.words2vec.count()}")
	println("Preparing tweets")
	PrepareTweets.registerPreparedTweetsTempTable()
	
	// we need an ActorSystem to host our application in
	implicit val system = ActorSystem("redRock")

	// create and start our service actor
	val service = system.actorOf(Props[MyServiceActor], Config.restName)

	implicit val timeout = Timeout(500.seconds)
	
	IO(Http) ? Http.Bind(service, interface = "localhost", port = Config.port)

	println(s"Application: ${Config.appName} running version: ${Config.appVersion}")
}
