
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
  val vmactor: ActorRef = system.actorOf(Props(classOf[VMActor], router ,2 seconds, 20 milliseconds))

  // Bind to an HTTP port and handle incoming messages.
  // With the custom extractor we're always certain the header contains
  // the correct upgrade message.
  // We can pass in a socketoptions to tune the buffer behavior
  // e.g options =  List(Inet.SO.SendBufferSize(100))
  val binding = Http().bindAndHandleSync({

//    case WSRequest(req@HttpRequest(GET, Uri.Path("/simple"), _, _, _)) => handleWith(req, Flows.reverseFlow)
//    case WSRequest(req@HttpRequest(GET, Uri.Path("/echo"), _, _, _)) => handleWith(req, Flows.echoFlow)
//    case WSRequest(req@HttpRequest(GET, Uri.Path("/graph"), _, _, _)) => handleWith(req, Flows.graphFlow)
//    case WSRequest(req@HttpRequest(GET, Uri.Path("/graphWithSource"), _, _, _)) => handleWith(req, Flows.graphFlowWithExtraSource)
    case WSRequest(req@HttpRequest(GET, Uri.Path("/stats"), _, _, _)) => handleWith(req, Flows.graphFlowWithStats(router))
    case _: HttpRequest => HttpResponse(400, entity = "Invalid websocket request")

  }, interface = "localhost", port = 9008)



  // binding is a future, we assume it's ready within a second or timeout
  try {
    Await.result(binding, 1 second)
    println("Server online at http://localhost:9001")
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
   * Simple flow which just returns the original message
   * back to the client
   */
  def echoFlow: Flow[Message, Message, Unit] =  Flow[Message]

  /**
   * Flow which uses a graph to process the incoming message.
   *
   *                           compute
   *  collect ~> broadcast ~>  compute ~> zip ~> map
   *                           compute
   *
   * We broadcast the message to three map functions, we
   * then zip them all up, and map them to the response
   * message which we return.
   *
   * @return
   */
  def graphFlow: Flow[Message, Message, Unit] = {
    Flow() { implicit b =>

      import FlowGraph.Implicits._

      val collect = b.add(Flow[Message].collect[String]({
        case TextMessage.Strict(txt) => txt
      }))

      // setup the components of the flow
      val compute1 = b.add(Flow[String].map(_ + ":1"))
      val compute2 = b.add(Flow[String].map(_ + ":2"))
      val compute3 = b.add(Flow[String].map(_ + ":3"))

      val broadcast = b.add(Broadcast[String](3))
      val zip = b.add(ZipWith[String,String,String,String]((s1, s2, s3) => s1 + s2 + s3))
      val mapToMessage = b.add(Flow[String].map[TextMessage](TextMessage.Strict))

      // now we build up the flow
                 broadcast ~> compute1 ~> zip.in0
      collect ~> broadcast ~> compute2 ~> zip.in1
                 broadcast ~> compute3 ~> zip.in2

      zip.out ~> mapToMessage

      (collect.inlet, mapToMessage.outlet)
    }
  }

  /**
   * When the flow is materialized we don't really just have to respond with a single
   * message. Any message that is produced from the flow gets sent to the client. This
   * means we can also attach an additional source to the flow and use that to push
   * messages to the client.
   *
   * So this flow looks like this:
   *
   *        in ~> filter ~> merge
   *           newSource ~> merge ~> map
   * This flow filters out the incoming messages, and the merge will only see messages
   * from our new flow. All these messages get sent to the connected websocket.
   *
   *
   * @return
   */
  def graphFlowWithExtraSource: Flow[Message, Message, Unit] = {
    Flow() { implicit b =>
      import FlowGraph.Implicits._

      // Graph elements we'll use
      val merge = b.add(Merge[Int](2))
      val filter = b.add(Flow[Int].filter(_ => false))

      // convert to int so we can connect to merge
      val mapMsgToInt = b.add(Flow[Message].map[Int] { msg => -1 })
      val mapIntToMsg = b.add(Flow[Int].map[Message]( x => TextMessage.Strict(":" + randomPrintableString(200) + ":" + x.toString)))
      val log = b.add(Flow[Int].map[Int](x => {println(x); x}))

      // source we want to use to send message to the connected websocket sink
      val rangeSource = b.add(Source(1 to 2000))

      // connect the graph
      mapMsgToInt ~> filter ~> merge // this part of the merge will never provide msgs
         rangeSource ~> log ~> merge ~> mapIntToMsg

      // expose ports
      (mapMsgToInt.inlet, mapIntToMsg.outlet)
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

  def randomPrintableString(length: Int, start:String = ""): String = {
    if (length == 0) start else randomPrintableString(length -1, start + Random.nextPrintableChar())
  }
}
