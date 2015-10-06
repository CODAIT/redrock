/**
 * (C) Copyright IBM Corp. 2015, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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