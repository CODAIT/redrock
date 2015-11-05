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
package com.restapi

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import org.slf4j.LoggerFactory;


object Application extends App {
    val logger = LoggerFactory.getLogger(this.getClass)
    /*Starting REST API*/
    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem(LoadConf.restConf.getString("actor"))
    // create and start our service actor
    val service = system.actorOf(Props[MyServiceActor], LoadConf.restConf.getString("name"))
    val sessionTimeout = system.actorOf(Props[SessionTimeoutActor])
    val sessionTable = system.actorOf(Props(classOf[SimpleSession], sessionTimeout, LoadConf.accessConf.getInt("delay") seconds, LoadConf.accessConf.getInt("timeout-interval") seconds))
    sessionTable ! InitSessionTable
    val sessionLoader = system.actorOf(Props(classOf[LoadSessionActor], sessionTable, LoadConf.accessConf.getInt("delay") seconds, LoadConf.accessConf.getInt("check-interval") seconds))
    sessionLoader ! InitFileMd5Sum
    implicit val timeout = Timeout(800.seconds)
    IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = LoadConf.restConf.getInt("port"))

    logger.info(s"""Application: ${LoadConf.globalConf.getString("appName")} running version: ${LoadConf.globalConf.getString("appVersion")}""")
}
