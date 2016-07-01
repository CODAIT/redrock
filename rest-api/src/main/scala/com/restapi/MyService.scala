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

import akka.actor.Actor
import com.restapi.ExecuteSentimentAnalysis._
import com.restapi.SignOutMsg
import spray.http.HttpHeaders.`Content-Type`
import spray.routing._
import spray.http._
import MediaTypes._
import Directives._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.http.DateTime
import org.slf4j.LoggerFactory

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {
  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender ! HttpResponse(StatusCodes.InternalServerError, "Too late")
  }

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context // scalastyle:ignore

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = handleTimeouts orElse runRoute(myRoute) // scalastyle:ignore
}


// create route for HTTP request
trait MyService extends HttpService {
  val logger = LoggerFactory.getLogger(this.getClass)

  val home = pathPrefix("ss")
  val search = path("search") & parameters('termsInclude, 'termsExclude,
    'top.as[Int] ?, 'user, 'startDate ?, 'endDate ?)
  val sentiment = pathPrefix("sentiment")
  val sentimentAnalysis = path("analysis") & parameters('termsInclude, 'termsExclude, 'sentiment.as[Int], 'user, 'startDatetime, 'endDatetime, 'top.as[Int]) // scalastyle:ignore
  // scalastyle:ignore
  val powertrack = pathPrefix("powertrack")
  val wordcount = path("wordcount") & parameters('user, 'batchSize.as[Int],
    'topTweets.as[Int], 'topWords.as[Int], 'termsInclude, 'termsExclude)
  val auth = pathPrefix("auth")
  val signin = path("signin") & parameters('user)
  val signout = path("signout") & parameters('user)
  implicit val timeout = Timeout(2.second)
  val complementaryDeniedMsg = LoadConf.accessConf.getString("complementaryDeniedMessage")

  val myRoute =
    home {
      search { (includeTerms, excludeTerms, top, user, startDate, endDate) =>
        get {
          logger.info(s"""REST Server request: search by: $user""")
          if (LoadConf.accessConf.getString("enable") == "on") {
            clientIP {
              ip => {
                ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                val f: Future[Any] = Application.sessionTable ?
                  SessionCheckMsg(user.toLowerCase(), ip.toString(), DateTime.now)

                val response = f flatMap {
                  case SessionCheckResultMsg(msg) => {
                    if (msg) {
                      val search = ExecuteSearchRequest.runSearchAnalysis(includeTerms,
                        excludeTerms,
                        top.getOrElse(LoadConf.restConf.getInt("searchParam.defaultTopTweets")),
                        startDate.getOrElse(LoadConf.restConf.getString("searchParam.defaulStartDatetime")), // scalastyle:ignore
                        endDate.getOrElse(LoadConf.restConf.getString("searchParam.defaultEndDatetime")), // scalastyle:ignore
                        user)
                      search map (x => HttpResponse(StatusCodes.OK, entity = x, headers = List(`Content-Type`(`application/json`)))) // scalastyle:ignore
                    } else {
                      Future(HttpResponse(StatusCodes.OK,
                        s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                    }
                  }
                  case _ => {
                    Future(HttpResponse(StatusCodes.OK,
                      s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                  }
                }
                complete {
                  response
                }
              }
            }
          }
          else {
            respondWithMediaType(`application/json`) {
              complete {
                ExecuteSearchRequest.runSearchAnalysis(includeTerms, excludeTerms,
                  top.getOrElse(LoadConf.restConf.getInt("searchParam.defaultTopTweets")),
                  startDate.getOrElse(LoadConf.restConf.getString("searchParam.defaulStartDatetime")), // scalastyle:ignore
                  endDate.getOrElse(LoadConf.restConf.getString("searchParam.defaultEndDatetime")),
                  user)
              }
            }
          }
        }
      } ~
        sentiment {
          sentimentAnalysis { (termsInclude, termsExclude, sentiment,
                               user, startDatetime, endDatetime, top) =>
            get {
              logger.info(s"""REST Server request: sentiment/sentimentAnalysis by: $user""")
              if (LoadConf.accessConf.getString("enable") == "on") {
                clientIP {
                  ip => {
                    ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                    val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user.toLowerCase(), ip.toString(), DateTime.now) // scalastyle:ignore
                    val response = f flatMap {
                      case SessionCheckResultMsg(msg) => {
                        if (msg) {
                          val search = runSentimentAnalysis(termsInclude, termsExclude,
                            top, startDatetime, endDatetime, sentiment, user)
                          Future(HttpResponse(StatusCodes.OK, entity = search, headers = List(`Content-Type`(`application/json`)))) // scalastyle:ignore
                        } else {
                          Future(HttpResponse(StatusCodes.OK,
                            s"""{"success":false,"message":"User $user is not authorized. $complementaryDeniedMsg"}""".stripMargin)) // scalastyle:ignore
                        }
                      }
                      case _ => {
                        Future(HttpResponse(StatusCodes.OK,
                          s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                      }
                    }
                    complete {
                      response
                    }
                  }
                }

              } else {
                respondWithMediaType(`application/json`) {
                  complete {
                    runSentimentAnalysis(termsInclude, termsExclude, top,
                      startDatetime, endDatetime, sentiment, user)
                  }
                }
              }
            }
          }
        } ~
        powertrack {
          wordcount { (user, batchSize, topTweets, topWords, termsInclude, termsExclude) =>
            get {
              logger.info(s"""REST Server request: powertrack/wordcount by: $user""")
              if (LoadConf.accessConf.getString("enable") == "on") {
                clientIP {
                  ip => {
                    ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                    val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user.toLowerCase(), ip.toString(), DateTime.now) // scalastyle:ignore
                    val response = f flatMap {
                      case SessionCheckResultMsg(msg) => {
                        if (msg) {
                          val search = ExecutePowertrackRequest.runPowertrackAnalysis(batchSize,
                            topTweets, topWords, termsInclude, termsExclude, user)
                          search map (x => HttpResponse(StatusCodes.OK, entity = x, headers = List(`Content-Type`(`application/json`)))) // scalastyle:ignore
                        } else {
                          Future(HttpResponse(StatusCodes.OK,
                            s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                        }
                      }
                      case _ => {
                        Future(HttpResponse(StatusCodes.OK,
                          s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                      }
                    }
                    complete {
                      response
                    }
                  }
                }
              } else {
                respondWithMediaType(`application/json`) {
                  complete {
                    ExecutePowertrackRequest.runPowertrackAnalysis(batchSize, topTweets,
                      topWords, termsInclude, termsExclude, user)
                  }
                }
              }
            }
          }
        } ~
        auth {
          signin { (user) =>
            get {
              logger.info(s"""REST Server request: auth/signin by: $user""")
              clientIP {
                ip => {
                  ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                  val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user.toLowerCase(), ip.toString(), DateTime.now) // scalastyle:ignore

                  val response = f flatMap {
                    case SessionCheckResultMsg(msg) => {
                      if (msg) {
                        Future {
                          HttpResponse(StatusCodes.OK,
                            s"""{"success":true, "message":"User $user signed in!"}""")
                        }
                      } else {
                        Future(HttpResponse(StatusCodes.OK,
                          s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                      }
                    }
                    case _ => {
                      Future(HttpResponse(StatusCodes.OK,
                        s"""{"success":false, "message":"User $user is not authorized. $complementaryDeniedMsg"}""")) // scalastyle:ignore
                    }
                  }
                  complete {
                    response
                  }
                }
              }
            }
          } ~
            signout { (user) =>
              get {
                logger.info(s"""REST Server request: auth/signout by: $user""")
                Application.sessionTable ! SignOutMsg(user.toLowerCase())
                complete {
                  HttpResponse(StatusCodes.OK,
                    s"""{"success":true, "message":"User $user signed out!"}""")
                }
              }
            }
        }
    }
}
