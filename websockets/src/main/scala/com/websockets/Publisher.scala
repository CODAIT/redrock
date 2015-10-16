
package com.websockets

import java.io.File

import akka.actor._
import akka.routing.{RemoveRoutee, ActorRefRoutee, AddRoutee}
import akka.routing._
import akka.stream.actor.ActorPublisher
import play.api.libs.json.Json
import scala.annotation.tailrec
import scala.concurrent.duration._

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

/**
 * Just a simple router, which collects some VM stats and sends them to the provided
 * actorRef each interval.
 */
class VMActor(router: ActorRef, delay: FiniteDuration, interval: FiniteDuration) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  context.system.scheduler.schedule(delay, interval) {
    val json = Json.obj( "stats" -> getStats.map(el => el._1 -> el._2))
    router ! Json.prettyPrint(json)
  }

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }

  def getStats: Map[String, Long] = {

    val baseStats = Map[String, Long](
      "count.procs" -> Runtime.getRuntime.availableProcessors(),
      "count.mem.free" -> Runtime.getRuntime.freeMemory(),
      "count.mem.maxMemory" -> Runtime.getRuntime.maxMemory(),
      "count.mem.totalMemory" -> Runtime.getRuntime.totalMemory()
    )

    val roots = File.listRoots()
    val totalSpaceMap = roots.map(root => s"count.fs.total.${root.getAbsolutePath}" -> root.getTotalSpace) toMap
    val freeSpaceMap = roots.map(root => s"count.fs.free.${root.getAbsolutePath}" -> root.getFreeSpace) toMap
    val usuableSpaceMap = roots.map(root => s"count.fs.usuable.${root.getAbsolutePath}" -> root.getUsableSpace) toMap

    baseStats ++ totalSpaceMap ++ freeSpaceMap ++ usuableSpaceMap
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
