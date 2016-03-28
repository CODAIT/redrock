/**
 * (C) Copyright IBM Corp. 2015, 2016
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

import org.slf4j.LoggerFactory
import play.api.libs.json._
import java.io._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, future, Await}

object ExecuteSearchRequest {

  val logger = LoggerFactory.getLogger(this.getClass)

  def runSearchAnalysis(includeTerms: String, excludeTerms: String, top: Int,
                        startDate: String, endDate: String, user: String): Future[String] = {
    logger.info("Processing search:")
    logger.info(s"User: $user")
    logger.info("Include: " + includeTerms)
    logger.info("Exclude: " + excludeTerms)

    executeAsynchronous(top, includeTerms.toLowerCase(), excludeTerms.toLowerCase(),
      startDate, endDate) map { js =>
      Json.stringify(js)
    }
  }

  def executeAsynchronous(top: Int, includeTerms: String, excludeTerms: String,
                          startDate: String, endDate: String): Future[JsValue] = {
    val cluster_distance: Future[JsValue] = future {
      extracTopWordDistance(includeTerms, excludeTerms)
    }
    val elasticsearch_dataAnalisys: Future[JsValue] =
      extractElasticsearchAnalysis(top, includeTerms, excludeTerms, startDate, endDate)

    val tasks: Seq[Future[JsValue]] = Seq(elasticsearch_dataAnalisys, cluster_distance)
    val aggregated: Future[Seq[JsValue]] = Future.sequence(tasks)
    val result = aggregated.map(jsonResults => Json.obj("success" -> true) ++
      jsonResults(0).as[JsObject] ++ jsonResults(1).as[JsObject])

    result.recover {
      case e: Exception =>
        logger.error("Execute Python and ES Asynchronous", e)
        emptyJSONResponse()
    }

  }

  def extractElasticsearchAnalysis(top: Int, includeTerms: String, excludeTerms: String,
                                   startDate: String, endDate: String): Future[JsValue] = {

    val elasticsearchRequests = new GetElasticsearchResponse(top,
      includeTerms.toLowerCase().trim().split(","),
      excludeTerms.toLowerCase().trim().split(","), startDate,
      endDate, LoadConf.esConf.getString("decahoseIndexName"))

    val totalTweets: Future[JsObject] = future {
      formatTotalTweets(elasticsearchRequests)
    }
    val totalUsersAndFilteresTweets: Future[JsObject] = future {
      formatTotalFilteredTweetsAndTotalUsers(elasticsearchRequests)
    }
    val sentiment: Future[JsObject] = future {
      formatSentiment(elasticsearchRequests)
    }
    val professions: Future[JsObject] = future {
      formatProfession(elasticsearchRequests)
    }
    val location: Future[JsObject] = future {
      formatLocation(elasticsearchRequests)
    }
    val topTweets: Future[JsObject] = future {
      formatTopTweets(elasticsearchRequests)
    }

    val tasks: Seq[Future[JsObject]] = Seq(topTweets, totalTweets,
      totalUsersAndFilteresTweets, sentiment, professions, location)

    val aggregated: Future[Seq[JsObject]] = Future.sequence(tasks)
    val initialJson: JsObject = Json.obj("status" -> 0)
    val result = aggregated.map(jsonResults => initialJson ++ jsonResults(0) ++
      jsonResults(1) ++ jsonResults(2) ++ jsonResults(3) ++
      jsonResults(4) ++ jsonResults(5))

    result.recover {
      case e: Exception =>
        logger.error("Extract Elasticsearch Analysis", e)
        emptyJSONResponseES()
    }
  }

  def formatTotalTweets(elasticsearchRequests: GetElasticsearchResponse): JsObject = {
    try {
      val totalTweetsResponse = CurrentTotalTweets.totalTweets
      return Json.obj("totaltweets" -> totalTweetsResponse)
    } catch {
      case e: Exception => logger.error("Format Total Tweets", e)
    }

    return Json.obj("totaltweets" -> JsNull)
  }

  def formatTotalFilteredTweetsAndTotalUsers(elasticsearchRequests: GetElasticsearchResponse): JsObject = { // scalastyle:ignore
    try {
      /* The Elasticsearch query used to calculate unique values executes a Cardinality Aggregation.
       * This query requires the pre-compute of the field hash on indexing time.
       * This is a really expensive query and you can experience slow performance on a big dataset.
       * If you are using a big dataset, please make sure that your ES cluster reflects the size of the data.
       */
      val countResponse = Json.parse(elasticsearchRequests.getTotalFilteredTweetsAndTotalUserResponse())
      return ( Json.obj( "totalfilteredtweets" -> (countResponse \ "hits" \ "total")) ++
      Json.obj( "totalusers" ->
      (countResponse \ "aggregations" \ "distinct_users_by_id" \ "value")))
    } catch {
      case e: Exception => logger.error("Get Total Filtered Tweets And Total Users", e)
    }

    return Json.obj("totalfilteredtweets" -> JsNull, "totalusers" -> JsNull)
  }

  def formatTopTweets(elasticsearchRequests: GetElasticsearchResponse): JsObject = {
    try {
      val topTweetsResponse = Json.parse(elasticsearchRequests.getTopTweetsResponse())
      var sortedTweets = ((topTweetsResponse \ "hits" \ "hits").as[List[JsObject]])

      if (LoadConf.restConf.getBoolean("validateTweetsBeforeDisplaying")) {
        val tweetsID = Json.obj("messages" ->
          sortedTweets.map(tweet => (tweet \ "_source" \ "tweet_id")))
        val nonCompliantTweets = ValidateTweetCompliance.getNonCompliantTweets(Json.stringify(tweetsID)) // scalastyle:ignore
        if (!nonCompliantTweets.isEmpty) {
          sortedTweets = sortedTweets.filter(tweet => !nonCompliantTweets.contains((tweet \ "_source" \ "tweet_id").as[String])) // scalastyle:ignore
        }
      }

      val validatedTweets = sortedTweets.map(tweet => {
        Json.obj("created_at" -> (tweet \ "_source" \ "created_at"),
          "text" -> (tweet \ "_source" \ "tweet_text"),
          "user" -> Json.obj(
            "name" -> (tweet \ "_source" \ "user_name"),
            "screen_name" -> (tweet \ "_source" \ "user_handle"),
            "followers_count" -> (tweet \ "_source" \ "user_followers_count"),
            "id" -> (tweet \ "_source" \ "user_id"),
            "profile_image_url" -> (tweet \ "_source" \ "user_image_url")
          )
        )
      })

      return Json.obj("toptweets" -> Json.obj("tweets" -> validatedTweets))
    }
    catch {
      case e: Exception => logger.error("Get Top Tweets", e)
    }

    return Json.obj("toptweets" -> Json.obj("tweets" -> JsNull))
  }

  def formatProfession(elasticsearchRequests: GetElasticsearchResponse): JsObject = {
    try {
      val professionsResponse = Json.parse(elasticsearchRequests.getProfessionResponse())
      val professionGroups = (professionsResponse \ "aggregations" \ "tweet_professions" \ "professions" \ "buckets").as[List[JsObject]] // scalastyle:ignore

      val mapProfessions = professionGroups.map(professionGroup => {
        val profession = professionGroup \ "key"
        val professionKeywords: List[JsObject] = ((professionGroup \ "keywords" \ "buckets").as[List[JsObject]]).map(keyword => { // scalastyle:ignore
          Json.obj("name" -> (keyword \ "key"), "value" -> (keyword \ "doc_count"))
        })

        Json.obj("name" -> profession, "children" -> professionKeywords)
      })

      return Json.obj("profession" -> Json.obj("profession" -> mapProfessions))
    }
    catch {
      case e: Exception => logger.error("Format Professions", e)
    }

    return Json.obj("profession" -> JsNull)
  }

  def formatLocation(elasticsearchRequests: GetElasticsearchResponse): JsObject = {
    try {
      val locationResponse = Json.parse(elasticsearchRequests.getLocationResponse())
      val locationTimestampGroups = (locationResponse \ "aggregations" \ "tweet_cnt" \ "buckets").as[List[JsObject]] // scalastyle:ignore

      val mapLocations = locationTimestampGroups.map(locationGroup => {
        val timestamp = locationGroup \ "key_as_string"
        val locationCount = (locationGroup \ "tweet_locat" \ "buckets").as[List[JsObject]]
        locationCount.map(locationData => {
          Json.arr(timestamp, (locationData \ "key"), (locationData \ "doc_count"))
        })
      }).flatten

      return Json.obj("location" -> Json.obj("fields" ->
        Json.arr("Date", "Country", "Count"), "location" -> mapLocations))
    }
    catch {
      case e: Exception => logger.error("Format Location", e)
    }

    return Json.obj("location" -> Json.obj("fields" ->
      Json.arr("Date", "Country", "Count"), "location" -> JsNull))
  }

  def formatSentiment(elasticsearchRequests: GetElasticsearchResponse): JsObject = {
    try {
      val sentimentResponse = Json.parse(elasticsearchRequests.getSentimentResponse())
      val sentTimestampGroups = (sentimentResponse \ "aggregations" \ "tweet_cnt" \ "buckets").as[List[JsObject]] // scalastyle:ignore

      val transformedData = sentTimestampGroups.map(sentTimestampGroup => {
        val timestamp = sentTimestampGroup \ "key_as_string"
        val sentimentCount = (sentTimestampGroup \ "tweet_sent" \ "buckets").as[List[JsObject]]
        // Not all search is going to return result for all sentiments (1,-1,0)
        val listSent = sentimentCount.map(sentimentData => ((sentimentData \ "key").as[Long],
          (sentimentData \ "doc_count").as[Long])).toMap

        val values: (Long, Long, Long) = (listSent.getOrElse(1, 0),
          listSent.getOrElse(-1, 0), listSent.getOrElse(0, 0))

        Json.arr(timestamp, values._1, values._2, values._3)
      })

      return Json.obj("sentiment" -> Json.obj("fields" ->
        Json.arr("Date", "Positive", "Negative", "Neutral"),
        "sentiment" -> transformedData))

    } catch {
      case e: Exception => logger.error("Format Sentiment", e)
    }

    return Json.obj("sentiment" -> Json.obj("fields" ->
      Json.arr("Date", "Positive", "Negative",
        "Neutral"), "sentiment" -> JsNull))
  }

  /* ########## Word Distance and Cluster Python Program ############ */

  def extracTopWordDistance(includeTerms: String, excludeTerms: String): JsValue = {
    try {
      val startTime = System.nanoTime()
      val cmd = Array[String](LoadConf.restConf.getString("python-code.pythonVersion"),
        LoadConf.restConf.getString("python-code.classPath"), includeTerms,
        excludeTerms, LoadConf.globalConf.getString("homePath"))

      // create runtime to execute external command
      val rt: Runtime = Runtime.getRuntime()
      val pr: Process = rt.exec(cmd)

      // retrieve output from python script
      val stdOutput: BufferedReader = new BufferedReader(new InputStreamReader(pr.getInputStream()))
      val stdError: BufferedReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()))
      val error = stdError.readLine()
      if (error != null) {
        logger.error(error)
        var error_ite = stdError.readLine()
        while (error_ite != null) {
          logger.error(error_ite)
          error_ite = stdError.readLine()
        }
      }
      else {
        // If no errors occurs, Json will be printed in the first line of th program
        val elapsed = (System.nanoTime() - startTime) / 1e9
        logger.info(s"Caculate Cluster and Distance (sec): $elapsed")
        return Json.parse(stdOutput.readLine())
      }

    } catch {
      case e: Exception => logger.error("Execute Python: Cluster and Distance", e)
    }
    return Json.obj("cluster" -> JsNull, "distance" -> JsNull)
  }

  /* ########################### Util ############################### */
  def emptyJSONResponse(): JsValue = {
    Json.obj("status" -> JsNull, "totaltweets" -> JsNull,
      "totalfilteredtweets" -> JsNull, "totalusers" -> JsNull,
      "profession" -> JsNull, "location" -> JsNull,
      "sentiment" -> JsNull, "toptweets" -> JsNull,
      "cluster" -> JsNull, "distance" -> JsNull, "success" -> true)
  }

  def emptyJSONResponseES(): JsValue = {
    Json.obj("status" -> JsNull, "totaltweets" -> JsNull,
      "totalfilteredtweets" -> JsNull, "totalusers" -> JsNull,
      "profession" -> JsNull, "location" -> JsNull,
      "sentiment" -> JsNull, "toptweets" -> JsNull)
  }

}
