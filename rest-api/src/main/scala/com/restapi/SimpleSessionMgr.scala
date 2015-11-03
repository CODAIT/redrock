
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
 * Three Acotors:
 * SimpleSession
 * SessionTimeoutActor
 * LoadSessionActor
 *
 * SimpleSession peridically sends timeout check msg (all sessions) to SessionTimeoutActor;
 * SessionTimeoutActor checks the session table and return a list of expired users and update SimpleSession
 * LoadSessionActor peridically checks whether the configuration file defined by "access-list" field in
 * rest-api configuration file.
 * If access-list is changed, it loads a new sessionTable and updates SimpleSession.
 * SimpleSession updates its local session table: Add new entries and remove old entries based on the LoadSessionActor
 * Access-list file Format:
 * line1: user1@gmail.com
 * line2: user2@ibm.com
 * ......
 */
package com.restapi

import java.io.{File, FileInputStream}

import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import spray.http.DateTime
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.codec.digest.DigestUtils
import scala.io.Source

case class MapUpdateMsg(Msg: Map[String, (String, DateTime)])
case class TimeoutMsg(Msg: List[String])

trait FileMd5Sum {
  def getFileMd5Sum(file: String) : String = {
    val fis: FileInputStream = new FileInputStream(new File(file))
    val md5: String = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis)
    fis.close()
    md5
  }
}

class SimpleSession(timeoutActor: ActorRef, delay: FiniteDuration, interval: FiniteDuration) extends Actor with FileMd5Sum {

  context.system.scheduler.schedule(delay, interval) {
    sendMsgToTimeoutAcotr()
  }

  var sessionTable: Map[String, (String, DateTime)] = Map.empty[String, (String, DateTime)]
  var fileMd5Sum: String=""
  var onlineUsers: Int = 0

  override def receive: Actor.Receive = {
    case MapUpdateMsg(msg) => {
      /*Add entry that is in msg but not in sessionTable*/
      for ((k, v) <- msg) {
        val result = sessionTable get k
        result match {
          case Some(t) =>
          case _ => sessionTable += (k -> v)
        }
      }
      /*Remove entry that is sessionTable but not in msg*/
      for ((k, v) <- sessionTable) {
        val result = msg get k
        result match {
          case Some(t) =>
          case _ => sessionTable -= k
        }
      }
    }
    case TimeoutMsg(msg) => {
      for (i <- msg) {
        sessionTable += (i -> Pair("", DateTime(1979, 1, 1, 0, 0, 0)))
        if (onlineUsers > 0) onlineUsers -= 1
      }
    }
    case _ =>
  }

  def updateSession (userid: String, ip: String, timestamp: DateTime) = {
    val result = sessionTable get userid
    result match {
      case Some(t) => sessionTable += (userid -> Pair(ip, timestamp))
      case _ =>
    }
  }

  def shouldAcceptSession(userid: String, ip: String, timestamp: DateTime) : Boolean = {
    val maxUsers:Int = LoadConf.restConf.getInt("max-allowed-users")
    var accept: Boolean = true
    if (onlineUsers > maxUsers) accept = false
    val user = sessionTable get userid
    user match {
      case Some(t) => {
        accept = true
        updateSession(userid, ip, timestamp)
      }
      case _ => accept = false
    }
    accept
  }

  def sendMsgToTimeoutAcotr (): Unit = {
    timeoutActor ! MapUpdateMsg(sessionTable)
  }
}

class SessionTimeoutActor () extends Actor  {
  override def receive: Actor.Receive = {
    case MapUpdateMsg(msg) => {
      sender ! TimeoutMsg(iterateSessionTable(msg))
    }
    case _ => // just ignore any other messages
  }

  def iterateSessionTable (sessions: Map[String, (String, DateTime)]): List[String] = {
    val timeoutList: List[String] = List.empty[String]
    val currentTime: Int = DateTime.now.second
    val sesnTimeout: Int = LoadConf.restConf.getInt("session-timeout")
    for(session <- sessions) {
      val lastUpdate: Int = session._2._2.second
      if ((currentTime - lastUpdate) > sesnTimeout) {
        session._1::timeoutList
      }
    }
    timeoutList
  }
}

class LoadSessionActor (sessionActor: ActorRef, delay: FiniteDuration, interval: FiniteDuration, timeout: FiniteDuration) extends Actor  with FileMd5Sum {
  context.system.scheduler.schedule(delay, interval) {
    updateSessionTable
  }

  private var current_md5: String = ""

  def initSessionFileMd5sum (): Unit = {
    current_md5 = getFileMd5Sum(LoadConf.restConf.getString("access-list"))
  }

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }

  def fileChanged : Boolean = {
    val lastest_md5: String = getFileMd5Sum(LoadConf.restConf.getString("access-list"))
    if (lastest_md5 == current_md5) {
      false
    } else {
      current_md5 = lastest_md5
      true
    }
  }

  def loadSessionTable (filename: String): Map[String, (String, DateTime)] = {
    val source = Source.fromFile(LoadConf.restConf.getString("access-list"))
    val lines = try source.getLines().toList finally source.close()
    var newSessionTable = Map.empty[String, (String, DateTime)]
    for (line <- lines) {
      newSessionTable += (line -> Pair("", DateTime(1979, 1, 1, 0, 0, 0)))
    }
    newSessionTable
  }

  def updateSessionTable = {
    if (fileChanged) {
      sessionActor ! MapUpdateMsg(loadSessionTable(LoadConf.restConf.getString("access-list")))
    }
  }
}
