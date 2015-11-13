
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
import org.slf4j.LoggerFactory
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
case class InitSessionTable()
case class SessionCheckMsg(userid: String, ip: String, timestamp: DateTime)
case class SessionUpdateMsg(userid: String, ip: String, timestamp: DateTime)
case class SessionCheckResultMsg(Msg: Boolean)
case class SignOutMsg(Msg: String)
case object InitFileMd5Sum

trait FileMd5Sum {
  val logger = LoggerFactory.getLogger(this.getClass)
  def getFileMd5Sum(file: String) : String = {
    val fis: FileInputStream = new FileInputStream(new File(file))
    val md5: String = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis)
    fis.close()
    md5
  }

  def loadSessionTable (filename: String): Map[String, (String, DateTime)] = {
    val source = Source.fromFile(LoadConf.accessConf.getString("access-list"))
    val lines = try source.getLines().toList finally source.close()
    var newSessionTable = Map.empty[String, (String, DateTime)]
    for (line <- lines) {
      newSessionTable += (line.toLowerCase() -> Pair("", DateTime(1979, 1, 1, 0, 0, 0)))
    }
    if(newSessionTable.size == 0) logger.error("Session Table loaded from "+ LoadConf.accessConf.getString("access-list")+ "is empty!")
    newSessionTable
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
          case _ => {
            sessionTable += (k -> v)
            logger.info("User "+k+" added to table.")
          }
        }
      }
      /*Remove entry that is sessionTable but not in msg*/
      for ((k, v) <- sessionTable) {
        val result = msg get k
        result match {
          case Some(t) =>
          case _ => {
            sessionTable -= k
            logger.info("User "+k+" is removed from table.")
          }
        }
      }
    }
    case TimeoutMsg(msg) => {
      logger.info("Receive Timeout Msg size "+msg.size)
      for (i <- msg) {
        sessionTable += (i -> Pair("", DateTime(1979, 1, 1, 0, 0, 0)))
        if (onlineUsers > 0) onlineUsers -= 1
        logger.info("User "+i+" timeout!"+"online users "+onlineUsers)
      }
    }
    case SignOutMsg(msg) => {
      logger.info("Receive user "+msg+" signout msg.")
      val result = sessionTable get msg
      result match {
        case Some(t) => {
          sessionTable += (msg -> Pair("", DateTime(1979, 1, 1, 0, 0, 0)))
          if (onlineUsers > 0) onlineUsers -= 1
          logger.info("User "+msg+" sign out. Online users: "+onlineUsers)
        }
        case _ => logger.info("User"+msg+" doesn't exist when sign out. Online users: "+onlineUsers)
      }
    }
    case InitSessionTable => initSessionTable()
    case SessionCheckMsg(userid, ip, timestamp) => {
      sender ! SessionCheckResultMsg(shouldAcceptSession(userid, ip, timestamp))
    }
    case SessionUpdateMsg(userid, ip, timestamp) => {
      updateSession(userid, ip, timestamp)
    }
    case _ =>
  }

  def updateSession (userid: String, ip: String, timestamp: DateTime) = {
    logger.info("User "+userid+" IP "+ip+" for update.")
    val result = sessionTable get userid
    result match {
      case Some(t) => sessionTable += (userid -> Pair(ip, timestamp))
      case _ =>
    }
  }

  def shouldAcceptSession(userid: String, ip: String, timestamp: DateTime) : Boolean = {
    logger.info("User "+userid+" IP "+ip+" for authentication.")
    val maxUsers:Int = LoadConf.accessConf.getInt("max-allowed-users")
    var accept: Boolean = true
    if (onlineUsers > maxUsers)
    {
      logger.info("User "+userid+" denied: Too many users online.")
      return false
    }
    val user = sessionTable get userid
    user match {
      case Some(t) => {
        accept = true
        if (t._1 == "") {
          onlineUsers += 1
        }
        updateSession(userid, ip, timestamp)
      }
      case _ => accept = false
    }
    if (accept) {
      logger.info("User "+userid+" accepted."+" online users: "+onlineUsers)
    } else {
      logger.info("User "+userid+" denied")
    }
    accept
  }

  def sendMsgToTimeoutAcotr (): Unit = {
    logger.info("Send Timeout Check Msg.")
    timeoutActor ! MapUpdateMsg(sessionTable)
  }

  def initSessionTable (): Unit = {
    sessionTable = loadSessionTable(LoadConf.accessConf.getString("access-list"))
  }
}

class SessionTimeoutActor () extends Actor  {
  val logger = LoggerFactory.getLogger(this.getClass)
  override def receive: Actor.Receive = {
    case MapUpdateMsg(msg) => {
      val timeoutList = iterateSessionTable(msg)
      if(timeoutList.size >0)
        sender ! TimeoutMsg(timeoutList)
    }
    case _ => // just ignore any other messages
  }

  def iterateSessionTable (sessions: Map[String, (String, DateTime)]): List[String] = {
    var timeoutList: List[String] = List.empty[String]
    val currentTime: Long = DateTime.now.clicks
    val sesnTimeout: Long = LoadConf.accessConf.getInt("session-timeout") * 1000
    for(session <- sessions) {
      if (session._2._1 != "") {
        val lastUpdate: Long = session._2._2.clicks
        logger.info("User "+session._1+" IP"+session._2._1+" last update "+lastUpdate+" Current Time "+currentTime)
        logger.info("Session size: "+sessions.size)
        if ((currentTime - lastUpdate) > sesnTimeout) {
          timeoutList = session._1 :: timeoutList
          logger.info("User "+session._1+" session is timeout. timeoutList size "+timeoutList.size)
        }
      }
    }
    timeoutList
  }
}

class LoadSessionActor (sessionActor: ActorRef, delay: FiniteDuration, interval: FiniteDuration) extends Actor  with FileMd5Sum {
  context.system.scheduler.schedule(delay, interval) {
    updateSessionTable
  }

  private var current_md5: String = ""

  def initSessionFileMd5sum (): Unit = {
    current_md5 = getFileMd5Sum(LoadConf.accessConf.getString("access-list"))
  }

  override def receive: Actor.Receive = {
    case InitFileMd5Sum => {
      initSessionFileMd5sum()
      logger.info("Init File Md5 Sum "+current_md5)
    }
    case _ => // just ignore any messages
  }

  def fileChanged : Boolean = {
    val latest_md5: String = getFileMd5Sum(LoadConf.accessConf.getString("access-list"))
    if (latest_md5 == current_md5) {
      false
    } else {
      logger.info("File Changed, new Md5 Sum "+latest_md5)
      current_md5 = latest_md5
      true
    }
  }

  def updateSessionTable = {
    if (fileChanged) {
      sessionActor ! MapUpdateMsg(loadSessionTable(LoadConf.accessConf.getString("access-list")))
      logger.info("Session Table Update Msg Sent.")
    }
  }
}
