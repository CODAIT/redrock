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

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {
  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender ! HttpResponse(StatusCodes.InternalServerError, "Too late")
  }
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = handleTimeouts orElse runRoute(myRoute)
}


// create route for HTTP request
trait MyService extends HttpService {

  val home = pathPrefix("ss")
  val search = path("search") & parameters('termsInclude, 'termsExclude, 'top.as[Int] ?, 'user, 'startDate ?, 'endDate ?)
  val sentiment = pathPrefix("sentiment")
  val sentimentAnalysis = path("analysis") & parameters('termsInclude, 'termsExclude, 'sentiment.as[Int] , 'user, 'startDatetime, 'endDatetime, 'top.as[Int])
  val powertrack = pathPrefix("powertrack")
  val wordcount = path("wordcount") & parameters('user, 'batchSize.as[Int], 'topTweets.as[Int], 'topWords.as[Int], 'termsInclude, 'termsExclude)
  val auth = pathPrefix("auth")
  val signin = path("signin") & parameters('user)
  val signout = path("signout") & parameters('user)
  implicit val timeout = Timeout(2.second)

  val myRoute =
    home {
      search { (includeTerms, excludeTerms, top, user, startDate, endDate) =>
        get {
          if (LoadConf.accessConf.getString("enable") == "on") {
            clientIP {
              ip => {
                ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user, ip.toString(), DateTime.now)

                val response = f flatMap {
                  case SessionCheckResultMsg(msg) => {
                    if (msg) {
                    val search = ExecuteSearchRequest.runSearchAnalysis(includeTerms, excludeTerms,
                      top.getOrElse(LoadConf.restConf.getInt("searchParam.defaultTopTweets")),
                      startDate.getOrElse(LoadConf.restConf.getString("searchParam.defaulStartDatetime")),
                      endDate.getOrElse(LoadConf.restConf.getString("searchParam.defaultEndDatetime")))
                      search map (x => HttpResponse(StatusCodes.OK, entity = x, headers = List(`Content-Type`(`application/json`))))
                    } else {
                      Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                    }
                  }
                  case _ => {
                    Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                  }
                }
                complete{response}
              }
            }
          }
          else {
            respondWithMediaType(`application/json`) {
              complete {
                ExecuteSearchRequest.runSearchAnalysis(includeTerms, excludeTerms,
                  top.getOrElse(LoadConf.restConf.getInt("searchParam.defaultTopTweets")),
                  startDate.getOrElse(LoadConf.restConf.getString("searchParam.defaulStartDatetime")),
                  endDate.getOrElse(LoadConf.restConf.getString("searchParam.defaultEndDatetime")))
              }
            }
          }
        }
      } ~
      sentiment {
        sentimentAnalysis { (termsInclude, termsExclude, sentiment, user, startDatetime, endDatetime, top) =>
          get{
            if (LoadConf.accessConf.getString("enable") == "on") {
              clientIP {
                ip => {
                  ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                  val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user, ip.toString(), DateTime.now)
                  val response = f flatMap {
                  case SessionCheckResultMsg(msg) => {
                    if (msg) {
                      val search = runSentimentAnalysis(termsInclude, termsExclude, top, startDatetime, endDatetime, sentiment)
                      Future(HttpResponse(StatusCodes.OK, entity = search, headers = List(`Content-Type`(`application/json`))))
                    } else {
                      Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                    }
                  }
                  case _ => {
                    Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                  }
                }
                  complete{response}
                }
              }

            } else {
              respondWithMediaType(`application/json`) {
                complete {
                  runSentimentAnalysis(termsInclude, termsExclude, top, startDatetime, endDatetime, sentiment)
                }
              }
            }
          }
        }
      } ~
      powertrack {
        wordcount { (user, batchSize, topTweets, topWords, termsInclude, termsExclude) =>
          get {
            if (LoadConf.accessConf.getString("enable") == "on") {
              clientIP {
                ip => {
                  ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                  val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user, ip.toString(), DateTime.now)
                  val response = f flatMap {
                  case SessionCheckResultMsg(msg) => {
                    if (msg) {
                      val search = ExecutePowertrackRequest.runPowertrackAnalysis(batchSize, topTweets, topWords, termsInclude, termsExclude)
                      search map (x => HttpResponse(StatusCodes.OK, entity = x, headers = List(`Content-Type`(`application/json`))))
                    } else {
                      Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                    }
                  }
                  case _ => {
                    Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                  }
                }
                  complete{response}
                }
              }
            } else {
              respondWithMediaType(`application/json`) {
                complete {
                  ExecutePowertrackRequest.runPowertrackAnalysis(batchSize, topTweets, topWords, termsInclude, termsExclude)
                }
              }
            }
          }
        }
      }~
      auth {
        signin { (user) =>
          get {
            clientIP {
              ip => {
                ip.toOption.map(_.getHostAddress).getOrElse("unknown")

                val f: Future[Any] = Application.sessionTable ? SessionCheckMsg(user, ip.toString(), DateTime.now)

                val response = f flatMap {
                  case SessionCheckResultMsg(msg) => {
                    if (msg) {
                      Future{HttpResponse(StatusCodes.OK, s"""{"success":true, "message":"User $user signed in!"}""")}
                    } else {
                      Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                    }
                  }
                  case _ => {
                    Future(HttpResponse(StatusCodes.InternalServerError, s"""{"success":false, "message":"User $user is not authorized!"}"""))
                  }
                }
                complete{response}
              }
            }
          }
        }~
        signout { (user) =>
          get {
            Application.sessionTable ! SignOutMsg(user)
            complete{HttpResponse(StatusCodes.OK, s"""{"success":true, "message":"User $user signed out!"}""")}
          }
        }
      }
    }
}
