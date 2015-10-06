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
package com.redRock

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
import scala.io.Source._

class GetElasticsearchResponse(val topTweets: Int, includeTerms:Array[String], excludeTerms:Array[String])
{
	val baseURL = "http://" + Config.elasticsearchIP + ":" + Config.elasticsearchPort + "/" + Config.esIndex + "/" + Config.esTable
	val searchURL = baseURL + "/_search"
	val countURL = searchURL + "?search_type=count"
	val includeTermsES = includeTerms.mkString(" ")
	val excludeTermsES = excludeTerms.mkString(" ")

	def getTopTweetsResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getTopTweetsJSONRequest(includeTermsES, excludeTermsES, topTweets)
		return performSearch(searchURL, jsonRequest)
	}

	def getLocationResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getLocationJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(countURL, jsonRequest)
	}

	def getSentimentResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getSentimentJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(countURL, jsonRequest)
	}

	def getProfessionResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getProfessionJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(countURL, jsonRequest)
	}

	def getTotalTweetsESResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getTotalTweetsJSONRequest()
		return performSearch(countURL, jsonRequest)
	}

	def getTotalFilteredTweetsAndTotalUserResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getTotalFilteredTweetsAndTotalUserJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(countURL, jsonRequest)
	}

	def performSearch(url: String, jsonQueryRequest:String): String = {
		try
		{
			val httpClient = new DefaultHttpClient()
	    	val request = new HttpPost(url)
	    	request.setEntity(new StringEntity(jsonQueryRequest) )

	    	val httpResponse = httpClient.execute(request)
	    	val entity = httpResponse.getEntity()
	    	var jsonResponse = ""
	    	if (entity != null) {
	      		val inputStream = entity.getContent()
	      		jsonResponse = fromInputStream(inputStream).getLines.mkString
	      		inputStream.close
	    	}

	    	httpClient.getConnectionManager().shutdown()
			return jsonResponse
		} catch {
			case e: Exception => {
				printException(e, "Retrieve ElasticSearch Response")
				return ""
			}
		}
	}

	def performSearchWithElasticSearchAPI(jsonQueryRequest:String): String = {
		try
		{
			val settings = ImmutableSettings.settingsBuilder()
			      .put("cluster.name", "elasticsearch")
			      .build();

	    	val client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(Config.elasticsearchIP, Config.elasticsearchApiPort.toInt));

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
				printException(e, "Retrieve ElasticSearch Response")
				return ""
		  }
		}
	}

	def printException(thr: Throwable, module: String) =
	{
			println("Exception on: " + module)
			val sw = new StringWriter
			thr.printStackTrace(new PrintWriter(sw))
			println(sw.toString)
	}
}
