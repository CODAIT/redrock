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

import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}
import org.apache.commons.lang.time.DateUtils
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future,future, Await}
import play.api.libs.json._


/**
 * Created by barbaragomes on 10/16/15.
 */
object ExecutePowertrackRequest {

  def runPowertrackAnalysis(batchTime: Int, topTweets: Int, topWords: Int, termsInclude: String, termsExclude: String): Future[String] =
  {
    Future {

      val (startDate, endDate) = getStartAndEndDateAccordingBatchTime(batchTime)
      println("Powertrack request")
      println(s"UTC start date: $startDate")
      println(s"UTC end date: $endDate")
      println(s"Included terms: $termsInclude")
      println(s"Excluded terms: $termsExclude")

      val elasticsearchResponse = new GetElasticsearchResponse(topTweets, termsInclude.toLowerCase().trim().split(","), termsExclude.toLowerCase().trim().split(","), startDate,  endDate, LoadConf.esConf.getString("powertrackType"))
      val wordCountJson = getTweetsAndWordCount(elasticsearchResponse, topWords)
      val totalUserAndTweetsJson = getUsersAndTweets(elasticsearchResponse)

      Json.stringify((wordCountJson ++ totalUserAndTweetsJson).as[JsValue])

    }.recover {
      case e: Exception =>
        Utils.printException(e, "Execute Powertrack Word Count");
        Json.stringify(Json.obj("toptweets" -> Json.obj("tweets" -> JsNull), "wordCount" -> JsNull,"totalfilteredtweets" -> JsNull, "totalusers" -> JsNull))
    }
  }

  def getUsersAndTweets(elasticsearchResponse: GetElasticsearchResponse): JsObject =
  {
    try {
      val countResponse = Json.parse(elasticsearchResponse.getTotalFilteredTweetsAndTotalUserResponse())
      return (Json.obj("totalfilteredtweets" -> (countResponse \ "hits" \ "total")) ++
        Json.obj("totalusers" -> (countResponse \ "aggregations" \ "distinct_users_by_id" \ "value")))
    }
    catch {
      case e: Exception => Utils.printException(e, "Powertrack user and tweets count")
        Json.obj("totalfilteredtweets" -> JsNull, "totalusers" -> JsNull)
    }
  }

  def getTweetsAndWordCount(elasticsearchResponse: GetElasticsearchResponse, topWords: Int): JsObject =
  {
    try
    {
      val response = Json.parse(elasticsearchResponse.getPowertrackTweetsAndWordCount(topWords))

      val tweets = ((response \ "hits" \ "hits").as[List[JsObject]]).map(tweet => {
        Json.obj(
          "created_at" -> (tweet \ "_source" \ "created_at"),
          "text" -> (tweet \ "_source" \ "tweet_text"),
          "user" -> Json.obj(
            "name" -> (tweet \ "_source" \ "user_name"),
            "screen_name" -> (tweet \ "_source" \ "user_handle"),
            "followers_count" -> (tweet \ "_source" \ "user_followers_count"),
            "id" -> (tweet \ "_source" \ "user_id"),
            "profile_image_url" -> (tweet \ "_source" \ "user_image_url")
          ))
      }
      )

      val words =  ((response \ "aggregations" \ "top_words" \ "buckets").as[List[JsObject]]).map( wordCount =>{
        Json.arr((wordCount \ "key"), (wordCount \ "doc_count"))
      })

      Json.obj("toptweets" -> Json.obj("tweets" -> tweets), "wordCount" -> words)
    }
    catch {
        case e: Exception => Utils.printException(e, "Powertrack word count")
          Json.obj("toptweets" -> Json.obj("tweets" -> JsNull), "wordCount" -> JsNull)
      }
  }

  def getStartAndEndDateAccordingBatchTime(batchTime: Int): (String, String) = {
    //end date should be the current date
    val endDate = Calendar.getInstance().getTime()
    val startDate = DateUtils.addMinutes(endDate, -batchTime)

    //Powertrack datetime timezine: UTC
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val sdf: SimpleDateFormat = new SimpleDateFormat(LoadConf.globalConf.getString("spark.powertrack.tweetTimestampFormat"))
    (sdf.format(startDate), sdf.format(endDate))
  }
}