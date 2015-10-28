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
 * Code in this file is modified from:
 * http://www.smartjava.org/content/create-reactive-websocket-server-akka-streams
 */


package com.websockets

import java.io.File

import akka.actor._
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.routing.{RemoveRoutee, ActorRefRoutee, AddRoutee}
import akka.routing._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Merge, Source, FlowGraph, Flow}
import play.api.libs.json.Json
import scala.annotation.tailrec
import scala.concurrent.duration._
import scalaj.http._
import com.websockets._

class Channel(channel_id: String, router: ActorRef, actorSystem: ActorSystem) {

  val vmactor: ActorRef = actorSystem.actorOf(Props(classOf[ChannelVMActor], router , 10 seconds, 10 seconds, channel_id))
  /* If object Flow can be shared with multiple channels
  def channelFlowWithJson() : Flow[Message, Message, Unit] = {
    Flows.graphFlowWithStats(router)
  }
  */
  def graphFlowWithJson(): Flow[Message, Message, Unit] = {
    Flow() { implicit b =>
      import FlowGraph.Implicits._

      // create an actor source
      val source = Source.actorPublisher[String](Props(classOf[VMStatsPublisher],router))

      // Graph elements we'll use
      val merge = b.add(Merge[String](2))
      val filter = b.add(Flow[String].filter(_ => false))

      // convert to int so we can connect to merge
      val mapMsgToString = b.add(Flow[Message].map[String] { msg => "" })
      val mapStringToMsg = b.add(Flow[String].map[Message]( x => TextMessage.Strict(x)))

      val statsSource = b.add(source)

      // connect the graph
      mapMsgToString ~> filter ~> merge // this part of the merge will never provide msgs
      statsSource ~> merge ~> mapStringToMsg

      // expose ports
      (mapMsgToString.inlet, mapStringToMsg.outlet)
    }
  }
}

object Channel {
  def apply(channel_id: String, router: ActorRef)(implicit actorSystem: ActorSystem) = new Channel(channel_id, router, actorSystem)
}

object Channels {
  var channels : Map[String, Channel] = Map.empty[String, Channel]
  def findOrCreate(channel_id: String)(implicit actorSystem: ActorSystem) : Channel = channels.getOrElse(channel_id, createNewChannel(channel_id))

  private def createNewChannel (channel_id: String)(implicit actorSystem: ActorSystem) : Channel = {
    val router: ActorRef = actorSystem.actorOf(Props[RouterActor], "router")
    val channel = Channel(channel_id, router)
    channels += channel_id -> channel
    channel
  }
}

/**
 * for now a very simple actor, which keeps a separate buffer
 * for each subscriber. This could be rewritten to store the
 * vmstats in an actor somewhere centrally and pull them from there.
 *
 * Based on the standed publisher example from the akka docs.
 */
class VMStatsPublisher(router: ActorRef) extends ActorPublisher[String] {

  case class QueueUpdated()

  import akka.stream.actor.ActorPublisherMessage._
  import scala.collection.mutable

  val MaxBufferSize = 50
  val queue = mutable.Queue[String]()

  var queueUpdated = false;

  // on startup, register with routee
  override def preStart() {
    router ! AddRoutee(ActorRefRoutee(self))
  }

  // cleanly remove this actor from the router. To
  // make sure our custom router only keeps track of
  // alive actors.
  override def postStop(): Unit = {
    router ! RemoveRoutee(ActorRefRoutee(self))
  }

  def receive = {

    // receive new stats, add them to the queue, and quickly
    // exit.
    case stats: String  =>
      // remove the oldest one from the queue and add a new one
      if (queue.size == MaxBufferSize) queue.dequeue()
      queue += stats
      if (!queueUpdated) {
        queueUpdated = true
        self ! QueueUpdated
      }

    // we receive this message if there are new items in the
    // queue. If we have a demand for messages send the requested
    // demand.
    case QueueUpdated => deliver()

    // the connected subscriber request n messages, we don't need
    // to explicitely check the amount, we use totalDemand propery for this
    case Request(amount) =>
      deliver()

    // subscriber stops, so we stop ourselves.
    case Cancel =>
      context.stop(self)
  }

  /**
   * Deliver the message to the subscriber. In the case of websockets over TCP, note
   * that even if we have a slow consumer, we won't notice that immediately. First the
   * buffers will fill up before we get feedback.
   */
  @tailrec final def deliver(): Unit = {
    if (totalDemand == 0) {
      println(s"No more demand for: $this")
    }

    if (queue.size == 0 && totalDemand != 0) {
      // we can response to queueupdated msgs again, since
      // we can't do anything until our queue contains stuff again.
      queueUpdated = false
    } else if (totalDemand > 0 && queue.size > 0) {
      onNext(queue.dequeue())
      deliver()
    }
  }
}

class ChannelVMActor(router: ActorRef, delay: FiniteDuration, interval: FiniteDuration, searchString: String) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  context.system.scheduler.schedule(delay, interval) {
    val json = Json.obj("response" -> getResults)
    router ! Json.prettyPrint(json)
  }

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }

  def getResults : String = {
 //   val response: HttpResponse[String] = Http("http://bdavm155.svl.ibm.com:16666/ss/search?user=barbara&termsInclude=:&termsExclude=&top=100").asString
    val response: HttpResponse[String] = Http(searchString).asString
    response.body
  }
}

/**
 * Just a simple router, which collects some VM stats and sends them to the provided
 * actorRef each interval.
 */
class VMActor(router: ActorRef, delay: FiniteDuration, interval: FiniteDuration) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  context.system.scheduler.schedule(delay, interval) {
    val json = Json.obj("response" -> getResults)
    router ! Json.prettyPrint(json)
  }

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }

  def getResults : String = {
    val response: HttpResponse[String] = Http("http://bdavm155.svl.ibm.com:16666/ss/search?user=barbara&termsInclude=:&termsExclude=&top=100").asString

    response.body
  }
}

/**
 * Simple router where we can add and remove routee. This actor is not
 * immutable.
 */
class RouterActor extends Actor {
  var routees = Set[Routee]()

  def receive = {
    case ar: AddRoutee => routees = routees + ar.routee
    case rr: RemoveRoutee => routees = routees - rr.routee
    case msg => routees.foreach(_.send(msg, sender))
  }
}
