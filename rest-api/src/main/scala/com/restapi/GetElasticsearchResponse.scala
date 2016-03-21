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

import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import java.io.StringWriter
import java.io.PrintWriter
import org.apache.http.HttpEntity
import org.apache.http.entity.StringEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.slf4j.LoggerFactory
import scala.io.Source._


class GetElasticsearchResponse(val topTweets: Int, includeTerms: Array[String] = Array[String](),
                               excludeTerms: Array[String] = Array[String](),
                               val startDateTime: String, val endDateTime: String,
                               esIndex: String) {

  val baseURL = "http://" + LoadConf.esConf.getString("bindIP") +
    ":" + LoadConf.esConf.getString("bindPort") +
    "/" + esIndex + "/" + LoadConf.esConf.getString("esType")
  val searchURL = baseURL + "/_search"
  val countURL = searchURL + "?search_type=count"
  val includeTermsES = includeTerms.map(x => s""""${x.trim()}"""").mkString(",")
  val excludeTermsES = excludeTerms.map(x => s""""${x.trim()}"""").mkString(",")
  val logger = LoggerFactory.getLogger(this.getClass)

  logger.info(s"Base URL => $baseURL")

  def getTopTweetsResponse(): String = {
    val jsonRequest = GetJSONRequest.getTopTweetsJSONRequest(includeTermsES, excludeTermsES,
      topTweets, startDateTime, endDateTime)
    return performSearch(searchURL, jsonRequest, "Top Tweets")
  }

  def getLocationResponse(): String = {
    val jsonRequest = GetJSONRequest.getLocationJSONRequest(includeTermsES, excludeTermsES,
      startDateTime, endDateTime)
    return performSearch(countURL, jsonRequest, "Location")
  }

  def getSentimentResponse(): String = {
    val jsonRequest = GetJSONRequest.getSentimentJSONRequest(includeTermsES, excludeTermsES,
      startDateTime, endDateTime)
    return performSearch(countURL, jsonRequest, "Sentiment")

  }

  def getProfessionResponse(): String = {
    val jsonRequest = GetJSONRequest.getProfessionJSONRequest(includeTermsES, excludeTermsES,
      startDateTime, endDateTime)
    return performSearch(countURL, jsonRequest, "Profession")
  }

  def getTotalTweetsESResponse(): String = {
    val jsonRequest = GetJSONRequest.getTotalTweetsJSONRequest(startDateTime, endDateTime)
    return performSearch(countURL, jsonRequest, "Total Tweets")
  }

  def getTotalFilteredTweetsAndTotalUserResponse(): String = {
    val jsonRequest = GetJSONRequest.getTotalFilteredTweetsAndTotalUserJSONRequest(includeTermsES,
      excludeTermsES, startDateTime, endDateTime)
    return performSearch(countURL, jsonRequest, "Filtered and User Count")
  }

  def getSentimentWordAnalysis(sentiment: Int): String = {
    val jsonRequest = GetJSONRequest.getTweetsTextBySentimentAndDate(includeTermsES,
      excludeTermsES, startDateTime, endDateTime, sentiment)
    return performSearch(searchURL, jsonRequest, "ML Sentiment Analysis")
  }

  def getPowertrackTweetsAndWordCount(topWords: Int): String = {
    val jsonRequest = GetJSONRequest.getPowertrackWordCountAndTweets(includeTermsES,
      excludeTermsES, startDateTime, endDateTime, topTweets, topWords)
    return performSearch(searchURL, jsonRequest, "Powertrack Tweets Word Count")
  }

  def getTotalRetweets(): String = {
    val jsonRequest = GetJSONRequest.getTotalRetweets(includeTermsES, excludeTermsES,
      startDateTime, endDateTime)
    return performSearch(countURL, jsonRequest, "Total Retweets")
  }

  def getTotalFilteredTweets(includeTermCondition: String): String = {
    val jsonRequest = GetJSONRequest.getTotalFilteredTweets(includeTermsES, excludeTermsES,
      startDateTime, endDateTime, includeTermCondition)
    return performSearch(countURL, jsonRequest, "Total Filtered")
  }

  def performSearch(url: String, jsonQueryRequest: String, queryName: String): String = {
    try {
      val startTime = System.nanoTime()
      val httpClient = new DefaultHttpClient()
      val request = new HttpPost(url)
      request.setEntity(new StringEntity(jsonQueryRequest))

      val httpResponse = httpClient.execute(request)
      val entity = httpResponse.getEntity()
      var jsonResponse = ""
      if (entity != null) {
        val inputStream = entity.getContent()
        jsonResponse = fromInputStream(inputStream).getLines.mkString
        inputStream.close
      }

      httpClient.getConnectionManager().shutdown()
      val elapsed = (System.nanoTime() - startTime) / 1e9
      logger.info(s"ES response for $queryName (sec): $elapsed")
      return jsonResponse
    } catch {
      case e: Exception => {
        logger.error("Retrieve ElasticSearch Response", e)
        return ""
      }
    }
  }

  def performSearchWithElasticSearchAPI(jsonQueryRequest: String): String = {
    try {
      val settings = ImmutableSettings.settingsBuilder()
        .put("cluster.name", "elasticsearch")
        .build();

      val client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(
        LoadConf.esConf.getString("bindIP"),
        LoadConf.esConf.getString("bindAPIPort").toInt));

      val response = client
        .prepareSearch("redrock")
        .setTypes("processed_tweets")
        .setQuery(jsonQueryRequest)
        .setSize(60)
        .setExplain(true)
        .execute()
        .actionGet()

      val jsonResponse = response.toString

      client.close()

      return jsonResponse
    } catch {
      case e: Exception => {
        logger.error("Retrieve ElasticSearch Response", e)
        return ""
      }
    }
  }
}
