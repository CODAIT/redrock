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
 * The PUSH server implementation is based on the article below:
 * http://www.smartjava.org/content/create-reactive-websocket-server-akka-streams
 */

package com.websockets

import java.net.{SocketOptions, Inet4Address, InetAddress, Socket}
import java.util.concurrent.TimeoutException

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.io.Inet
import akka.routing.BroadcastGroup
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
 * Extractor to detect websocket messages. This checks whether the header
 * is available, and whether it contains an actual upgrade message.
 */
object WSRequest {

  def unapply(req: HttpRequest) : Option[HttpRequest] = {
    if (req.header[UpgradeToWebsocket].isDefined) {
      req.header[UpgradeToWebsocket] match {
        case Some(upgrade) => Some(req)
        case None => None
      }
    } else None
  }

}

/**
 * Simple websocket server using akka-http and akka-streams.
 *
 * Note that about 600 messages get queued up in the send buffer (on mac, 146988 is default socket buffer)
 */
object WSServer extends App {

  // required actorsystem and flow materializer
  implicit val system = ActorSystem("websockets")
  implicit val fm = ActorFlowMaterializer()

  // setup the actors for the stats
  // router: will keep a list of connected actorpublisher, to inform them about new stats.
  // vmactor: will start sending messages to the router, which will pass them on to any
  // connected routee
  val router: ActorRef = system.actorOf(Props[RouterActor], "router")
  val vmactor: ActorRef = system.actorOf(Props(classOf[VMActor], router , 10 seconds, 10 seconds))

  // Bind to an HTTP port and handle incoming messages.
  // With the custom extractor we're always certain the header contains
  // the correct upgrade message.
  // We can pass in a socketoptions to tune the buffer behavior
  // e.g options =  List(Inet.SO.SendBufferSize(100))
  val binding = Http().bindAndHandleSync({
    case WSRequest(req@HttpRequest(GET, Uri.Path("/stats"), _, _, _)) => handleWith(req, Flows.graphFlowWithStats(router))
    case _: HttpRequest => HttpResponse(400, entity = "Invalid websocket request")

  }, interface = "localhost", port = 9008)



  // binding is a future, we assume it's ready within a second or timeout
  try {
    Await.result(binding, 1 second)
    println("Server online at http://localhost:9008")
  } catch {
    case exc: TimeoutException =>
      println("Server took to long to startup, shutting down")
      system.shutdown()
  }

  /**
   * Simple helper function, that connects a flow to a specific websocket upgrade request
   */
  def handleWith(req: HttpRequest, flow: Flow[Message, Message, Unit]) = req.header[UpgradeToWebsocket].get.handleMessages(flow)

}

/**
 * This object contains the flows the handle the websockets messages. Each flow is attached
 * to a websocket and gets executed whenever a message comes in from the client.
 */
object Flows {

  /**
   * The simple flow just reverses the sent message and returns it to the client. There
   * are two types of messages, streams and normal textmessages. We only process the
   * normal ones here, and ignore the others.
   */
  def reverseFlow: Flow[Message, Message, Unit] = {
    Flow[Message].map {
      case TextMessage.Strict(txt) => TextMessage.Strict(txt.reverse)
      case _ => TextMessage.Strict("Not supported message type")
    }
  }

  /**
   * Creates a flow which uses the provided source as additional input. This complete scenario
   * works like this:
   *  1. When the actor is created it registers itself with a router.
   *  2. the VMActor sends messages at an interval to the router.
   *  3. The router next sends the message to this source which injects it into the flow
   */
 def graphFlowWithStats(router: ActorRef): Flow[Message, Message, Unit] = {
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
