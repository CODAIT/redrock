
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

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import spray.http.DateTime
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global

object sessionTable {
  val sessionTable: Map[String, (String, DateTime)] = Map.empty[String, (String, DateTime)]
  var fileMd5Sum: String=""
}

class SimpleSession(implicit actorSystem: ActorSystem) {
  def updateSession (userid: String, ip: String, timestamp: DateTime) = {

  }

  def shouldAcceptSession(userid: String, ip: String, timestamp: DateTime) : Boolean = {
    true
  }

  def initSessionTable () {}

}

class SessionTimeoutActor (delay: FiniteDuration, interval: FiniteDuration, timeout: FiniteDuration) extends Actor  {
  context.system.scheduler.schedule(delay, interval) {
    println("Timer")
  }

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }

  def iterateSessionTable {}
}

class LoadSessionActor (delay: FiniteDuration, interval: FiniteDuration, timeout: FiniteDuration) extends Actor  {
  context.system.scheduler.schedule(delay, interval) {
    println("Timer")
  }

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }

  def getMd5Sum (filename: String) : String = {
    "empty"
  }

  def fileChanged : Boolean = {
    true
  }

  def updateSessionTable {}
}
