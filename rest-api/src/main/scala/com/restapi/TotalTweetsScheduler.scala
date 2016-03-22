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

import java.io.{File, FileInputStream}

import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.io.IO
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import spray.can.Http
import akka.pattern.ask
import spray.http.DateTime
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.codec.digest.DigestUtils
import scala.io.Source

case object GetTotalTweetsScheduler

object CurrentTotalTweets {
  @volatile
  var totalTweets: Long = 0
}

class ExecuterTotalTweetsES(delay: FiniteDuration, interval: FiniteDuration) extends Actor {
  context.system.scheduler.schedule(delay, interval) {
    getTotalTweetsES
  }

  val logger = LoggerFactory.getLogger(this.getClass)

  override def receive: Actor.Receive = {
    case GetTotalTweetsScheduler => {
      logger.info(s"Getting Total of Tweets. Begin: ${CurrentTotalTweets.totalTweets}")
    }
    case _ => // just ignore any messages
  }

  def getTotalTweetsES: Unit = {
    val elasticsearchRequests = new GetElasticsearchResponse(0, Array[String](), Array[String](),
      LoadConf.restConf.getString("searchParam.defaulStartDatetime"),
      LoadConf.restConf.getString("searchParam.defaultEndDatetime"),
      LoadConf.esConf.getString("decahoseIndexName"))
    val totalTweetsResponse = Json.parse(elasticsearchRequests.getTotalTweetsESResponse())
    logger.info(s"Getting Total of Tweets. Current: ${CurrentTotalTweets.totalTweets}")
    CurrentTotalTweets.totalTweets = (totalTweetsResponse \ "hits" \ "total").as[Long]
    logger.info(s"Total users updated. New: ${CurrentTotalTweets.totalTweets}")
  }
}

