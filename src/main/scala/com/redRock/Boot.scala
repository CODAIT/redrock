package com.redRock

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object Boot extends App {
	
	println("Loading location info")
	val (cities, country) = LoadLocationData.loadLocation()
	println("Loading profession info")
	val professions = ProfessionInfo.loadProfessionsTable()
	println("Registering analysis function")
	AnalysisFunction.registerAnalysisFunctions(cities, country, professions)
	println("Preparing tweets")
	PrepareTweets.registerPreparedTweetsTempTable()
	
	// we need an ActorSystem to host our application in
	implicit val system = ActorSystem("on-spray-can")

	// create and start our service actor
	val service = system.actorOf(Props[MyServiceActor], Config.restName)

	implicit val timeout = Timeout(500.seconds)
	// start a new HTTP server on port 8080 with our service actor as the handler
	IO(Http) ? Http.Bind(service, interface = "localhost", port = Config.port)

	println(s"Application: ${Config.appName} running version: ${Config.appVersion}")
}
