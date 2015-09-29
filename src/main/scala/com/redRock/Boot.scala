package com.redRock

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

object Boot extends App {

    println("Registering analysis function")
    AnalysisFunction.registerAnalysisFunctions()
    
    /* Load historical data and start tweets streaming */
    PrepareTweets.loadHistoricalDataAndStartStreaming()

    /*Starting REST API*/
    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("redRock")
    // create and start our service actor
    val service = system.actorOf(Props[MyServiceActor], Config.restName)
    implicit val timeout = Timeout(800.seconds)
    IO(Http) ? Http.Bind(service, interface = "localhost", port = Config.port)
    println(s"Application: ${Config.appName} running version: ${Config.appVersion}")

}