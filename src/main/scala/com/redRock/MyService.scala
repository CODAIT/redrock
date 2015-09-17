package com.redRock

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import Directives._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

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
  val search = path("search") & parameters('termsInclude, 'termsExclude, 'top.as[Int] ?, 'user )

  val myRoute =
    home {
      search { (includeTerms, excludeTerms, top, user) =>
        get{
          respondWithMediaType(`application/json`) {
            complete {
                ExecuteSearchRequest.runSearchAnalysis(includeTerms, excludeTerms, top.getOrElse(Config.defaultTop))
            }
          }
        }
      }
    }
}